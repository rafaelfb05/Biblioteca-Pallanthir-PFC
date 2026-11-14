import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import "./App.css";
import ModalDocumento from "./Componentes/ModalDocumento";
import { carregarVersaoDosTermos } from "./services/termos";
import {
  avaliacoesApi,
  carrinhoApi,
  dashboardApi,
  definirAoExpirarSessao,
  definirToken,
  lerDadosDoToken,
  livrosApi,
  logsApi,
  pedidosApi,
  reservasApi,
  tokenExpirado,
  usuariosApi,
} from "./services/api";

const ICONES_MATERIA = {
  TI: "💻",
  DIREITO: "⚖️",
  MEDICINA: "🩺",
  ODONTOLOGIA: "🦷",
  VETERINARIA: "🐾",
  FISICA: "🔭",
  QUIMICA: "🧪",
  ARQUITETURA: "🏛️",
  BIOLOGIA: "🧬",
};

const FORM_LIVRO_VAZIO = {
  titulo: "",
  materia: "",
  preco: "",
  estoqueReserva: "1",
  estoqueCompra: "0",
};
const FORM_USUARIO_VAZIO = {
  nome: "",
  email: "",
  senha: "",
  confirmarSenha: "",
  tipo: "ESTUDANTE",
  codigoAcesso: "",
  aceitouTermos: false,
};
const FORM_LOGIN_VAZIO = { email: "", senha: "" };
const FORM_RECUPERACAO_VAZIO = {
  email: "",
  codigo: "",
  novaSenha: "",
  confirmarSenha: "",
};
const FORM_CONTA_VAZIO = { nome: "", email: "" };
const FORM_AVALIACAO_VAZIO = { nota: 0, comentario: "" };
const RESUMO_AVALIACOES_VAZIO = { media: 0, total: 0, avaliacoes: [] };
const LIMITE_COMENTARIO = 500;
const NOTAS = [1, 2, 3, 4, 5];
const FILTRO_LOGS_VAZIO = { usuarioId: "", texto: "", acao: "", de: "", ate: "" };
const LIVROS_POR_PAGINA = 18;

const paginasVisiveis = (atual, total) => {
  if (total <= 7) {
    return Array.from({ length: total }, (_, i) => i + 1);
  }
  const paginas = [1];
  const inicio = Math.max(2, atual - 1);
  const fim = Math.min(total - 1, atual + 1);
  if (inicio > 2) {
    paginas.push("…");
  }
  for (let numero = inicio; numero <= fim; numero++) {
    paginas.push(numero);
  }
  if (fim < total - 1) {
    paginas.push("…");
  }
  paginas.push(total);
  return paginas;
};
const CHAVE_SESSAO = "pallanthir:usuario";
const FORMATO_UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

const numeroDoPedido = (id) => String(id).slice(0, 8).toUpperCase();

const iniciaisDoNome = (nome) =>
  String(nome || "")
    .trim()
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((parte) => parte[0].toUpperCase())
    .join("") || "?";

const formatarIsbn13 = (isbn) => {
  if (!isbn) {
    return "—";
  }
  return /^97[89]\d{10}$/.test(isbn) ? isbn.slice(0, 3) + "-" + isbn.slice(3) : isbn;
};
const CHAVE_ULTIMA_ATIVIDADE = "pallanthir:ultimaAtividade";
const TEMPO_LIMITE_INATIVIDADE = 15 * 60 * 1000;
const RENOVAR_TOKEN_ANTES_DE = 5 * 60 * 1000;
const INTERVALO_VERIFICACAO_SESSAO = 30 * 1000;
const INTERVALO_MINIMO_ATIVIDADE = 5 * 1000;
const EVENTOS_DE_ATIVIDADE = [
  "mousemove",
  "mousedown",
  "keydown",
  "scroll",
  "touchstart",
];
const MENSAGEM_INATIVIDADE =
  "Você foi desconectado após 15 minutos sem atividade.";

const salvarSessao = (sessao) => {
  try {
    localStorage.setItem(CHAVE_SESSAO, JSON.stringify(sessao));
  } catch (e) {
    console.error(e);
  }
};

const salvarUltimaAtividade = (momento) => {
  try {
    localStorage.setItem(CHAVE_ULTIMA_ATIVIDADE, String(momento));
  } catch (e) {
    console.error(e);
  }
};

const lerUltimaAtividade = () => {
  try {
    return Number(localStorage.getItem(CHAVE_ULTIMA_ATIVIDADE)) || 0;
  } catch (e) {
    return 0;
  }
};

const REGRA_SENHA = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@#$%^&+=!]).{8,}$/;
const DICA_SENHA =
  "Mínimo de 8 caracteres, com letra maiúscula, minúscula, número e um destes símbolos: @ # $ % ^ & + = !";

const ROTULO_TIPO_USUARIO = {
  ESTUDANTE: "Estudante",
  FUNCIONARIO: "Funcionário",
};

const TITULOS_AUTENTICACAO = {
  login: "Entrar",
  cadastro: "Criar conta",
  "2fa": "Verificação em duas etapas",
  recuperar: "Recuperar senha",
  redefinir: "Redefinir senha",
};

const PAGINAS_PRIVADAS = [
  "favoritos",
  "reservas",
  "carrinho",
  "pedidos",
  "conta",
  "gestao",
  "logs",
  "dashboard",
];

const formatarNumero = (valor) => Number(valor || 0).toLocaleString("pt-BR");

const formatarPorcentagem = (parte, total) =>
  total > 0 ? Math.round((parte / total) * 100) + "%" : "0%";

function GraficoMaterias({ titulo, descricao, itens, unidade }) {
  const maior = itens.reduce((max, item) => Math.max(max, item.quantidade), 0);

  return (
    <div className="dashboard-card">
      <h3>{titulo}</h3>
      <p className="dashboard-card-sub">{descricao}</p>
      {itens.length === 0 ? (
        <p className="dashboard-vazio">Ainda não há dados suficientes.</p>
      ) : (
        <ol className="dashboard-barras">
          {itens.map((item) => (
            <li
              key={item.nome}
              title={
                item.rotulo +
                ": " +
                formatarNumero(item.quantidade) +
                " " +
                unidade
              }
            >
              <span className="dashboard-barra-rotulo">
                <span aria-hidden="true">{ICONES_MATERIA[item.nome] || "📚"}</span>
                {item.rotulo}
              </span>
              <span className="dashboard-barra-trilho" aria-hidden="true">
                <span
                  className="dashboard-barra"
                  style={{
                    width:
                      maior > 0
                        ? Math.max((item.quantidade / maior) * 100, 2) + "%"
                        : "0%",
                  }}
                />
              </span>
              <span className="dashboard-barra-valor">
                {formatarNumero(item.quantidade)}
              </span>
            </li>
          ))}
        </ol>
      )}
    </div>
  );
}

const MENSAGEM_EXCLUSAO_BLOQUEADA =
  "Você possui livros reservados ou não devolvidos. Cancele as reservas ou devolva os livros na biblioteca antes de excluir a conta.";

const mensagemDeErro = (erro, padrao) =>
  erro && erro.status && erro.message ? erro.message : padrao;

const classeDaAcao = (acao) => {
  const texto = String(acao || "");
  if (/FALHA|NEGADO|BLOQUEADO/.test(texto)) {
    return "log-acao falha";
  }
  if (/EXCLUSAO|CANCELAMENTO/.test(texto)) {
    return "log-acao alerta";
  }
  if (/ACEITE/.test(texto)) {
    return "log-acao informacao";
  }
  return "log-acao sucesso";
};

const formatarEstoque = (quantidade) => {
  const total = Number(quantidade || 0);
  if (total <= 0) {
    return "Sem estoque";
  }
  return total + (total === 1 ? " unidade" : " unidades");
};

const ROTULO_STATUS_RESERVA = {
  RESERVADO: "Reservado",
  ENTREGUE: "Entregue",
  DEVOLVIDO: "Devolvido",
  CANCELADO: "Cancelado",
};

const CARRINHO_VAZIO = { itens: [], total: 0 };
const INTERVALO_ATUALIZACAO_PEDIDOS = 5 * 1000;
const MAXIMO_ATUALIZACOES_PEDIDOS = 24;

const ROTULO_STATUS_PEDIDO = {
  AGUARDANDO_PAGAMENTO: "Aguardando pagamento",
  APROVADO: "Aprovado",
  EXPIRADO: "Expirado",
};

const RETORNOS_DE_PAGAMENTO = {
  "/compra/sucesso": {
    tipo: "sucesso",
    aguardarConfirmacao: true,
    mensagem:
      "Recebemos o retorno do Mercado Pago. A confirmação do pagamento pode levar alguns instantes.",
  },
  "/compra/pendente": {
    tipo: "pendente",
    aguardarConfirmacao: true,
    mensagem:
      "Seu pagamento está pendente. O pedido será atualizado assim que o Mercado Pago confirmar.",
  },
  "/compra/falha": {
    tipo: "falha",
    aguardarConfirmacao: false,
    mensagem:
      "O pagamento não foi concluído. Você pode tentar novamente enquanto o pedido estiver aguardando pagamento.",
  },
};

const lerRetornoDePagamento = () =>
  RETORNOS_DE_PAGAMENTO[window.location.pathname.replace(/\/$/, "")] || null;

const estoqueDeCompra = (livro) => Number(livro.estoqueCompra || 0);

const podeSerComprado = (livro) =>
  estoqueDeCompra(livro) > 0 && Number(livro.preco || 0) > 0;

const formatarNota = (valor) => Number(valor || 0).toFixed(1);

const formatarTotalAvaliacoes = (quantidade) => {
  const total = Number(quantidade || 0);
  return total + (total === 1 ? " avaliação" : " avaliações");
};

const estrelasDaNota = (nota) => {
  const cheias = Math.round(Number(nota || 0));
  return "★".repeat(cheias) + "☆".repeat(5 - cheias);
};

const formatarData = (valor) => {
  if (!valor) {
    return "—";
  }
  const data = new Date(valor);
  if (Number.isNaN(data.getTime())) {
    return "—";
  }
  return data.toLocaleString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
};

const ZOOM_CAPA_CARTAO = 2;
const ZOOM_CAPA_PAGINA = 3;
const ZOOM_CAPA_AMPLIADA = 0;
const ESCALA_MINIMA = 1;
const ESCALA_MAXIMA = 5;
const PASSO_DA_ESCALA = 0.25;

const capaNaResolucao = (url, zoom) => {
  if (!url) {
    return url;
  }
  try {
    const endereco = new URL(url.replace(/^http:/, "https:"));
    if (!endereco.searchParams.has("zoom")) {
      return endereco.toString();
    }
    endereco.searchParams.set("zoom", String(zoom));
    endereco.searchParams.delete("edge");
    return endereco.toString();
  } catch (e) {
    return url;
  }
};

const limitarEscala = (valor) =>
  Math.min(
    ESCALA_MAXIMA,
    Math.max(ESCALA_MINIMA, Math.round(valor * 100) / 100)
  );

function CampoSenha({ rotulo, valor, aoMudar, autoComplete }) {
  const [visivel, setVisivel] = useState(false);

  return (
    <label>
      {rotulo}
      <div className="campo-senha">
        <input
          type={visivel ? "text" : "password"}
          required
          autoComplete={autoComplete}
          value={valor}
          onChange={(e) => aoMudar(e.target.value)}
        />
        <button
          type="button"
          className="mostrar-senha"
          onClick={() => setVisivel((atual) => !atual)}
          aria-label={visivel ? "Ocultar senha" : "Mostrar senha"}
          title={visivel ? "Ocultar senha" : "Mostrar senha"}
        >
          <svg
            viewBox="0 0 24 24"
            width="18"
            height="18"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden="true"
          >
            <path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7S2 12 2 12z" />
            <circle cx="12" cy="12" r="3" />
            {visivel && <path d="M4 4l16 16" />}
          </svg>
        </button>
      </div>
    </label>
  );
}

const REGRAS_DA_SENHA = [
  ["8 caracteres ou mais", (senha) => senha.length >= 8],
  ["Letra maiúscula", (senha) => /[A-Z]/.test(senha)],
  ["Letra minúscula", (senha) => /[a-z]/.test(senha)],
  ["Número", (senha) => /\d/.test(senha)],
  ["Símbolo (@ # $ % ^ & + = !)", (senha) => /[@#$%^&+=!]/.test(senha)],
];

function RegrasDaSenha({ senha, confirmacao }) {
  const texto = senha || "";
  const iguais = texto.length > 0 && texto === confirmacao;
  return (
    <ul className="regras-senha" aria-label="Requisitos da senha">
      {REGRAS_DA_SENHA.map(([descricao, atende]) => (
        <li key={descricao} className={atende(texto) ? "ok" : undefined}>
          <span aria-hidden="true">{atende(texto) ? "✓" : "•"}</span>
          {descricao}
        </li>
      ))}
      <li className={iguais ? "ok" : undefined}>
        <span aria-hidden="true">{iguais ? "✓" : "•"}</span>
        As duas senhas são iguais
      </li>
    </ul>
  );
}

function CapaDoItem({ capaUrl, titulo }) {
  const [falhou, setFalhou] = useState(false);

  if (!capaUrl || falhou) {
    return <CapaGenerica titulo={titulo} tamanho="mini" />;
  }
  return (
    <img
      src={capaNaResolucao(capaUrl, ZOOM_CAPA_CARTAO)}
      alt={"Capa de " + titulo}
      onError={() => setFalhou(true)}
    />
  );
}

function CapaGenerica({ titulo, tamanho }) {
  return (
    <div
      className={"capa-generica " + tamanho}
      role="img"
      aria-label={"Capa genérica de " + titulo}
    >
      <span className="capa-generica-titulo">{titulo}</span>
      <span className="capa-generica-marca">Pallanthir</span>
    </div>
  );
}

function VisualizadorDeCapa({ capaUrl, titulo, aoFechar }) {
  const [escala, setEscala] = useState(1);
  const [posicao, setPosicao] = useState({ x: 0, y: 0 });
  const [fonte, setFonte] = useState(
    capaNaResolucao(capaUrl, ZOOM_CAPA_AMPLIADA)
  );
  const arraste = useRef(null);

  const restaurar = useCallback(() => {
    setEscala(ESCALA_MINIMA);
    setPosicao({ x: 0, y: 0 });
  }, []);

  const ajustarEscala = useCallback((passo) => {
    setEscala((atual) => {
      const proxima = limitarEscala(atual + passo);
      if (proxima === ESCALA_MINIMA) {
        setPosicao({ x: 0, y: 0 });
      }
      return proxima;
    });
  }, []);

  useEffect(() => {
    const aoTeclar = (evento) => {
      if (evento.key === "Escape") {
        aoFechar();
      }
      if (evento.key === "+" || evento.key === "=") {
        ajustarEscala(PASSO_DA_ESCALA);
      }
      if (evento.key === "-" || evento.key === "_") {
        ajustarEscala(-PASSO_DA_ESCALA);
      }
      if (evento.key === "0") {
        restaurar();
      }
    };
    window.addEventListener("keydown", aoTeclar);
    const overflowAnterior = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      window.removeEventListener("keydown", aoTeclar);
      document.body.style.overflow = overflowAnterior;
    };
  }, [aoFechar, ajustarEscala, restaurar]);

  const iniciarArraste = (evento) => {
    if (escala === ESCALA_MINIMA) {
      return;
    }
    evento.preventDefault();
    arraste.current = {
      x: evento.clientX - posicao.x,
      y: evento.clientY - posicao.y,
    };
  };

  const moverArraste = (evento) => {
    if (!arraste.current) {
      return;
    }
    setPosicao({
      x: evento.clientX - arraste.current.x,
      y: evento.clientY - arraste.current.y,
    });
  };

  const encerrarArraste = () => {
    arraste.current = null;
  };

  const aoGirarRoda = (evento) => {
    ajustarEscala(evento.deltaY < 0 ? PASSO_DA_ESCALA : -PASSO_DA_ESCALA);
  };

  const aoClicarNoFundo = (evento) => {
    if (evento.target === evento.currentTarget) {
      aoFechar();
    }
  };

  return (
    <div
      className="visualizador-capa"
      role="dialog"
      aria-label={"Capa de " + titulo}
      onMouseDown={aoClicarNoFundo}
      onMouseMove={moverArraste}
      onMouseUp={encerrarArraste}
      onMouseLeave={encerrarArraste}
      onWheel={aoGirarRoda}
    >
      <div className="visualizador-barra">
        <span className="visualizador-titulo">{titulo}</span>
        <div className="visualizador-controles">
          <button
            type="button"
            onClick={() => ajustarEscala(-PASSO_DA_ESCALA)}
            disabled={escala <= ESCALA_MINIMA}
            aria-label="Diminuir zoom"
          >
            −
          </button>
          <span className="visualizador-escala">
            {Math.round(escala * 100)}%
          </span>
          <button
            type="button"
            onClick={() => ajustarEscala(PASSO_DA_ESCALA)}
            disabled={escala >= ESCALA_MAXIMA}
            aria-label="Aumentar zoom"
          >
            +
          </button>
          <button type="button" onClick={restaurar} aria-label="Restaurar zoom">
            ⟳
          </button>
          <button
            type="button"
            className="visualizador-fechar"
            onClick={aoFechar}
            aria-label="Fechar"
          >
            ✕
          </button>
        </div>
      </div>

      <div className="visualizador-palco">
        <img
          className={
            "visualizador-imagem" + (escala > ESCALA_MINIMA ? " movel" : "")
          }
          src={fonte}
          alt={"Capa de " + titulo}
          draggable="false"
          onMouseDown={iniciarArraste}
          onDoubleClick={() =>
            escala > ESCALA_MINIMA ? restaurar() : ajustarEscala(1)
          }
          onError={() => setFonte(capaNaResolucao(capaUrl, ZOOM_CAPA_PAGINA))}
          style={{
            transform:
              "translate(" +
              posicao.x +
              "px, " +
              posicao.y +
              "px) scale(" +
              escala +
              ")",
          }}
        />
      </div>

      <p className="visualizador-dica">
        Role a roda do mouse ou use os botões para ampliar, arraste para mover
        e pressione Esc para fechar.
      </p>
    </div>
  );
}

const formatarPreco = (valor) =>
  Number(valor || 0).toLocaleString("pt-BR", {
    style: "currency",
    currency: "BRL",
  });

function App() {
  const [livros, setLivros] = useState([]);
  const [materias, setMaterias] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState(null);
  const [aviso, setAviso] = useState(null);

  const [pagina, setPagina] = useState(() =>
    lerRetornoDePagamento() ? "pedidos" : "inicio"
  );
  const [busca, setBusca] = useState("");
  const [filtro, setFiltro] = useState({ tipo: "todos" });
  const [resultado, setResultado] = useState([]);

  const [usuario, setUsuario] = useState(null);
  const [favoritos, setFavoritos] = useState([]);
  const [favoritoEmEdicao, setFavoritoEmEdicao] = useState(null);
  const [reservas, setReservas] = useState([]);
  const [reservaEmEdicao, setReservaEmEdicao] = useState(null);
  const [carrinho, setCarrinho] = useState(CARRINHO_VAZIO);
  const [itemCarrinhoEmEdicao, setItemCarrinhoEmEdicao] = useState(null);
  const [finalizandoCompra, setFinalizandoCompra] = useState(false);
  const [pedidos, setPedidos] = useState([]);
  const [pedidoDetalhado, setPedidoDetalhado] = useState(null);
  const [carregandoPedidos, setCarregandoPedidos] = useState(true);
  const [erroPedidos, setErroPedidos] = useState(null);
  const [retornoPagamento, setRetornoPagamento] = useState(
    lerRetornoDePagamento
  );

  const [livroAbertoId, setLivroAbertoId] = useState(null);
  const [capaAmpliada, setCapaAmpliada] = useState(null);
  const [formEdicao, setFormEdicao] = useState(null);
  const [formLivro, setFormLivro] = useState(FORM_LIVRO_VAZIO);
  const [mostrarCadastroLivro, setMostrarCadastroLivro] = useState(false);
  const [mostrarAutenticacao, setMostrarAutenticacao] = useState(false);
  const [modoAutenticacao, setModoAutenticacao] = useState("login");
  const [formUsuario, setFormUsuario] = useState(FORM_USUARIO_VAZIO);
  const [formLogin, setFormLogin] = useState(FORM_LOGIN_VAZIO);
  const [formRecuperacao, setFormRecuperacao] = useState(FORM_RECUPERACAO_VAZIO);
  const [emailPendente, setEmailPendente] = useState("");
  const [codigo2FA, setCodigo2FA] = useState("");
  const [avisoAutenticacao, setAvisoAutenticacao] = useState(null);
  const [salvando, setSalvando] = useState(false);
  const [menuAberto, setMenuAberto] = useState(false);
  const [paginaLivros, setPaginaLivros] = useState(1);
  const [editandoLivro, setEditandoLivro] = useState(false);
  const [dataAceiteTermos, setDataAceiteTermos] = useState(null);
  const [termosPendentes, setTermosPendentes] = useState(null);
  const [termosLidos, setTermosLidos] = useState({ termos: false, privacidade: false });
  const [aceiteTermos, setAceiteTermos] = useState(false);
  const [erroTermos, setErroTermos] = useState(null);
  const [dialogoExclusao, setDialogoExclusao] = useState(null);
  const menuRef = useRef(null);
  const [documentoAberto, setDocumentoAberto] = useState(null);

  const [formConta, setFormConta] = useState(FORM_CONTA_VAZIO);
  const [usuariosGestao, setUsuariosGestao] = useState([]);
  const [filtroUsuarios, setFiltroUsuarios] = useState("");
  const [usuarioGestao, setUsuarioGestao] = useState(null);
  const [reservasGestao, setReservasGestao] = useState([]);
  const [logs, setLogs] = useState([]);
  const [dashboard, setDashboard] = useState(null);
  const [filtroLogs, setFiltroLogs] = useState(FILTRO_LOGS_VAZIO);
  const [carregandoPainel, setCarregandoPainel] = useState(false);

  const [resumoAvaliacoes, setResumoAvaliacoes] = useState(
    RESUMO_AVALIACOES_VAZIO
  );
  const [carregandoAvaliacoes, setCarregandoAvaliacoes] = useState(false);
  const [formAvaliacao, setFormAvaliacao] = useState(FORM_AVALIACAO_VAZIO);
  const [salvandoAvaliacao, setSalvandoAvaliacao] = useState(false);
  const [ranking, setRanking] = useState([]);
  const [carregandoRanking, setCarregandoRanking] = useState(false);
  const [erroRanking, setErroRanking] = useState(null);

  const usuarioId = usuario ? usuario.id : null;
  const ehFuncionario = Boolean(usuario && usuario.tipo === "FUNCIONARIO");
  const ehEstudante = Boolean(usuario && usuario.tipo === "ESTUDANTE");

  const idsFavoritos = useMemo(
    () => new Set(favoritos.map((livro) => livro.id)),
    [favoritos]
  );

  const nomeDaMateria = useCallback(
    (rotulo) => {
      const encontrada = materias.find(
        (m) => m.rotulo === rotulo || m.nome === rotulo
      );
      return encontrada ? encontrada.nome : "";
    },
    [materias]
  );

  const carregarLivros = useCallback(async () => {
    try {
      setCarregando(true);
      setErro(null);
      setLivros(await livrosApi.listar());
    } catch (e) {
      setErro(
        "Não foi possível carregar os livros. Verifique se o back-end está no ar."
      );
      console.error(e);
    } finally {
      setCarregando(false);
    }
  }, []);

  const carregarMaterias = useCallback(async () => {
    try {
      setMaterias(await livrosApi.listarMaterias());
    } catch (e) {
      console.error(e);
    }
  }, []);

  const carregarFavoritos = useCallback(async (id) => {
    try {
      setFavoritos(await usuariosApi.listarFavoritos(id));
    } catch (e) {
      console.error(e);
    }
  }, []);

  const carregarReservas = useCallback(async (id) => {
    try {
      setReservas(await reservasApi.listarPorUsuario(id));
    } catch (e) {
      console.error(e);
    }
  }, []);

  const carregarCarrinho = useCallback(async () => {
    try {
      setCarrinho((await carrinhoApi.obter()) || CARRINHO_VAZIO);
    } catch (e) {
      console.error(e);
    }
  }, []);

  const carregarPedidos = useCallback(async () => {
    try {
      setCarregandoPedidos(true);
      setPedidos((await pedidosApi.listar()) || []);
      setErroPedidos(null);
    } catch (e) {
      setErroPedidos(
        mensagemDeErro(e, "Não foi possível carregar os seus pedidos.")
      );
      console.error(e);
    } finally {
      setCarregandoPedidos(false);
    }
  }, []);

  const carregarAvaliacoes = useCallback(async (livroId) => {
    try {
      setCarregandoAvaliacoes(true);
      setResumoAvaliacoes(
        (await avaliacoesApi.resumo(livroId)) || RESUMO_AVALIACOES_VAZIO
      );
    } catch (e) {
      setResumoAvaliacoes(RESUMO_AVALIACOES_VAZIO);
      console.error(e);
    } finally {
      setCarregandoAvaliacoes(false);
    }
  }, []);

  const carregarRanking = useCallback(async () => {
    try {
      setCarregandoRanking(true);
      setErroRanking(null);
      setRanking((await avaliacoesApi.ranking()) || []);
    } catch (e) {
      setRanking([]);
      setErroRanking(
        mensagemDeErro(e, "Não foi possível carregar o ranking de avaliações.")
      );
      console.error(e);
    } finally {
      setCarregandoRanking(false);
    }
  }, []);

  const carregarDadosDoUsuario = useCallback(
    async (sessao) => {
      if (sessao.tipo !== "ESTUDANTE") {
        return;
      }
      await Promise.all([
        carregarFavoritos(sessao.id),
        carregarReservas(sessao.id),
        carregarCarrinho(),
        carregarPedidos(),
      ]);
    },
    [carregarFavoritos, carregarReservas, carregarCarrinho, carregarPedidos]
  );

  const ultimaAtividade = useRef(0);
  const renovandoToken = useRef(false);

  const encerrarSessao = useCallback((mensagem) => {
    try {
      localStorage.removeItem(CHAVE_SESSAO);
      localStorage.removeItem(CHAVE_ULTIMA_ATIVIDADE);
    } catch (e) {
      console.error(e);
    }
    definirToken(null);
    setUsuario(null);
    setFavoritos([]);
    setReservas([]);
    setCarrinho(CARRINHO_VAZIO);
    setPedidos([]);
    setErroPedidos(null);
    setRetornoPagamento(null);
    setUsuariosGestao([]);
    setUsuarioGestao(null);
    setReservasGestao([]);
    setLogs([]);
    setDashboard(null);
    setPagina((atual) => (PAGINAS_PRIVADAS.includes(atual) ? "inicio" : atual));
    setAviso(mensagem);
  }, []);

  const restaurarSessao = useCallback(async () => {
    try {
      const salvo = localStorage.getItem(CHAVE_SESSAO);
      if (!salvo) {
        return;
      }
      const sessao = JSON.parse(salvo);
      const ultima = lerUltimaAtividade();
      if (
        !sessao.token ||
        !FORMATO_UUID.test(String(sessao.id)) ||
        tokenExpirado(sessao.token) ||
        Date.now() - ultima >= TEMPO_LIMITE_INATIVIDADE
      ) {
        localStorage.removeItem(CHAVE_SESSAO);
        localStorage.removeItem(CHAVE_ULTIMA_ATIVIDADE);
        return;
      }
      ultimaAtividade.current = ultima;
      definirToken(sessao.token);
      setUsuario(sessao);
      await carregarDadosDoUsuario(sessao);
    } catch (e) {
      console.error(e);
    }
  }, [carregarDadosDoUsuario]);

  useEffect(() => {
    carregarLivros();
    carregarMaterias();
    restaurarSessao();
  }, [carregarLivros, carregarMaterias, restaurarSessao]);

  useEffect(() => {
    definirAoExpirarSessao(() =>
      encerrarSessao("Sua sessão expirou. Entre novamente.")
    );
  }, [encerrarSessao]);

  useEffect(() => {
    if (!usuario || !usuario.token) {
      return undefined;
    }
    const dados = lerDadosDoToken(usuario.token);
    if (!dados || !dados.exp) {
      return undefined;
    }
    const temporizador = setTimeout(
      () => encerrarSessao("Sua sessão expirou. Entre novamente."),
      Math.max(dados.exp * 1000 - Date.now(), 0)
    );
    return () => clearTimeout(temporizador);
  }, [usuario, encerrarSessao]);

  useEffect(() => {
    if (pagina !== "livro" || livroAbertoId === null) {
      return;
    }
    setResumoAvaliacoes(RESUMO_AVALIACOES_VAZIO);
    carregarAvaliacoes(livroAbertoId);
  }, [pagina, livroAbertoId, carregarAvaliacoes]);

  useEffect(() => {
    if (lerRetornoDePagamento()) {
      window.history.replaceState(null, "", "/");
    }
  }, []);

  const pedidoAguardando =
    pedidos.find((pedido) => pedido.status === "AGUARDANDO_PAGAMENTO") ||
    null;
  const temPedidoAguardando = Boolean(pedidoAguardando);
  const tinhaPedidoAguardando = useRef(false);

  useEffect(() => {
    if (tinhaPedidoAguardando.current && !temPedidoAguardando && ehEstudante) {
      carregarCarrinho();
    }
    tinhaPedidoAguardando.current = temPedidoAguardando;
  }, [temPedidoAguardando, ehEstudante, carregarCarrinho]);
  const aguardarConfirmacao = Boolean(
    pagina === "pedidos" &&
      ehEstudante &&
      temPedidoAguardando &&
      retornoPagamento &&
      retornoPagamento.aguardarConfirmacao
  );

  useEffect(() => {
    if (!aguardarConfirmacao) {
      return undefined;
    }
    let atualizacoes = 0;
    const intervalo = setInterval(() => {
      atualizacoes += 1;
      if (atualizacoes >= MAXIMO_ATUALIZACOES_PEDIDOS) {
        clearInterval(intervalo);
      }
      carregarPedidos();
    }, INTERVALO_ATUALIZACAO_PEDIDOS);
    return () => clearInterval(intervalo);
  }, [aguardarConfirmacao, carregarPedidos]);

  const minhaAvaliacao = useMemo(
    () =>
      (usuario &&
        (resumoAvaliacoes.avaliacoes || []).find(
          (avaliacao) => avaliacao.nomeUsuario === usuario.nome
        )) ||
      null,
    [usuario, resumoAvaliacoes]
  );

  useEffect(() => {
    setFormAvaliacao(
      minhaAvaliacao
        ? {
            nota: minhaAvaliacao.nota,
            comentario: minhaAvaliacao.comentario || "",
          }
        : FORM_AVALIACAO_VAZIO
    );
  }, [minhaAvaliacao]);

  const sessaoAtiva = Boolean(usuario);

  useEffect(() => {
    if (!sessaoAtiva) {
      return undefined;
    }
    const registrarAtividade = () => {
      const agora = Date.now();
      if (agora - ultimaAtividade.current < INTERVALO_MINIMO_ATIVIDADE) {
        return;
      }
      ultimaAtividade.current = agora;
      salvarUltimaAtividade(agora);
    };
    EVENTOS_DE_ATIVIDADE.forEach((evento) =>
      window.addEventListener(evento, registrarAtividade, { passive: true })
    );
    return () =>
      EVENTOS_DE_ATIVIDADE.forEach((evento) =>
        window.removeEventListener(evento, registrarAtividade)
      );
  }, [sessaoAtiva]);

  useEffect(() => {
    if (!usuario || !usuario.token) {
      return undefined;
    }

    const verificarSessao = async () => {
      const agora = Date.now();
      const ultima = Math.max(ultimaAtividade.current, lerUltimaAtividade());
      if (agora - ultima >= TEMPO_LIMITE_INATIVIDADE) {
        encerrarSessao(MENSAGEM_INATIVIDADE);
        return;
      }

      const dados = lerDadosDoToken(usuario.token);
      if (
        !dados ||
        !dados.exp ||
        renovandoToken.current ||
        dados.exp * 1000 - agora > RENOVAR_TOKEN_ANTES_DE
      ) {
        return;
      }

      try {
        renovandoToken.current = true;
        const resposta = await usuariosApi.renovarToken();
        const sessao = {
          ...usuario,
          nome: resposta.usuario.nome,
          token: resposta.token,
        };
        salvarSessao(sessao);
        definirToken(sessao.token);
        setUsuario(sessao);
      } catch (e) {
        console.error(e);
        if (e.status) {
          encerrarSessao("Sua sessão expirou. Entre novamente.");
        }
      } finally {
        renovandoToken.current = false;
      }
    };

    const intervalo = setInterval(verificarSessao, INTERVALO_VERIFICACAO_SESSAO);
    return () => clearInterval(intervalo);
  }, [usuario, encerrarSessao]);

  const trocarModoAutenticacao = (modo) => {
    setModoAutenticacao(modo);
    setErro(null);
    setAvisoAutenticacao(null);
  };

  const abrirAutenticacao = (modo) => {
    trocarModoAutenticacao(modo || "login");
    setMostrarAutenticacao(true);
  };

  const fecharAutenticacao = () => {
    setMostrarAutenticacao(false);
    setErro(null);
    setAvisoAutenticacao(null);
  };

  const iniciarSessao = async (resposta) => {
    const sessao = {
      id: resposta.usuario.id,
      nome: resposta.usuario.nome,
      email: resposta.usuario.email,
      tipo: resposta.usuario.tipo,
      token: resposta.token,
    };
    const agora = Date.now();
    ultimaAtividade.current = agora;
    salvarUltimaAtividade(agora);
    salvarSessao(sessao);
    definirToken(sessao.token);
    setUsuario(sessao);
    setMostrarAutenticacao(false);
    await carregarDadosDoUsuario(sessao);
  };

  const entrar = async (evento) => {
    evento.preventDefault();
    const email = formLogin.email.trim();

    try {
      setSalvando(true);
      setErro(null);
      setAvisoAutenticacao(null);
      await usuariosApi.login(email, formLogin.senha);
      setEmailPendente(email);
      setCodigo2FA("");
      setModoAutenticacao("2fa");
      setAvisoAutenticacao("Enviamos um código de acesso para o seu e-mail.");
    } catch (e) {
      setErro(mensagemDeErro(e, "Não foi possível entrar. Tente novamente."));
      console.error(e);
    } finally {
      setSalvando(false);
    }
  };

  const reenviarCodigo2FA = async () => {
    try {
      setSalvando(true);
      setErro(null);
      await usuariosApi.login(emailPendente, formLogin.senha);
      setAvisoAutenticacao("Enviamos um novo código para o seu e-mail.");
    } catch (e) {
      setErro(mensagemDeErro(e, "Não foi possível reenviar o código."));
      console.error(e);
    } finally {
      setSalvando(false);
    }
  };

  const confirmarCodigo2FA = async (evento) => {
    evento.preventDefault();

    try {
      setSalvando(true);
      setErro(null);
      const resposta = await usuariosApi.verificar2FA(
        emailPendente,
        codigo2FA.trim()
      );
      setFormLogin(FORM_LOGIN_VAZIO);
      setCodigo2FA("");
      setEmailPendente("");
      setAvisoAutenticacao(null);
      await iniciarSessao(resposta);
      setAviso("Bem-vindo(a), " + resposta.usuario.nome + "!");
    } catch (e) {
      setErro(mensagemDeErro(e, "Código inválido ou expirado."));
      console.error(e);
    } finally {
      setSalvando(false);
    }
  };

  const enviarCodigoRecuperacao = async (email) => {
    try {
      setSalvando(true);
      setErro(null);
      await usuariosApi.recuperarSenha(email);
      setFormRecuperacao({ ...FORM_RECUPERACAO_VAZIO, email });
      setModoAutenticacao("redefinir");
      setAvisoAutenticacao("Enviamos um código de recuperação para " + email + ".");
    } catch (e) {
      setErro(mensagemDeErro(e, "Não foi possível enviar o código."));
      console.error(e);
    } finally {
      setSalvando(false);
    }
  };

  const solicitarRecuperacao = async (evento) => {
    evento.preventDefault();
    await enviarCodigoRecuperacao(formRecuperacao.email.trim());
  };

  const redefinirSenha = async (evento) => {
    evento.preventDefault();

    if (!REGRA_SENHA.test(formRecuperacao.novaSenha)) {
      setErro(DICA_SENHA);
      return;
    }
    if (formRecuperacao.novaSenha !== formRecuperacao.confirmarSenha) {
      setErro("As senhas não conferem.");
      return;
    }

    try {
      setSalvando(true);
      setErro(null);
      await usuariosApi.redefinirSenha(
        formRecuperacao.email,
        formRecuperacao.codigo.trim(),
        formRecuperacao.novaSenha
      );
      const email = formRecuperacao.email;
      setFormRecuperacao(FORM_RECUPERACAO_VAZIO);
      if (usuario) {
        fecharAutenticacao();
        setAviso("Sua senha foi alterada.");
        return;
      }
      setFormLogin({ email, senha: "" });
      setModoAutenticacao("login");
      setAvisoAutenticacao("Senha redefinida. Entre com a nova senha.");
    } catch (e) {
      setErro(mensagemDeErro(e, "Não foi possível redefinir a senha."));
      console.error(e);
    } finally {
      setSalvando(false);
    }
  };

  const sair = () => {
    encerrarSessao("Você saiu da sua conta.");
  };

  const limparFiltro = () => {
    setFiltro({ tipo: "todos" });
    setResultado([]);
    setBusca("");
    setErro(null);
  };

  const abrirLivro = (livro) => {
    setPagina("livro");
    setLivroAbertoId(livro.id);
    setFormEdicao({ ...livro });
    setEditandoLivro(false);
    setErro(null);
    window.scrollTo({ top: 0 });
  };

  const voltarParaInicio = () => {
    setPagina("inicio");
    setLivroAbertoId(null);
    setFormEdicao(null);
    setErro(null);
    window.scrollTo({ top: 0 });
  };

  const irParaInicio = () => {
    voltarParaInicio();
    limparFiltro();
  };

  const abrirPagina = (nome) => {
    setPagina(nome);
    setLivroAbertoId(null);
    setFormEdicao(null);
    setErro(null);
    window.scrollTo({ top: 0 });
  };

  const irParaFavoritos = () => {
    abrirPagina("favoritos");
  };

  const irParaConta = () => {
    abrirPagina("conta");
    setFormConta({ nome: usuario.nome, email: usuario.email });
  };

  const carregarUsuariosGestao = async () => {
    try {
      setUsuariosGestao(await usuariosApi.listar());
    } catch (e) {
      setErro(mensagemDeErro(e, "Não foi possível carregar os usuários."));
      console.error(e);
    }
  };

  const irParaGestao = async () => {
    abrirPagina("gestao");
    setFiltroUsuarios("");
    await carregarUsuariosGestao();
  };

  const selecionarUsuarioGestao = async (selecionado) => {
    setUsuarioGestao(selecionado);
    setReservasGestao([]);
    try {
      setCarregandoPainel(true);
      setErro(null);
      setReservasGestao(await reservasApi.listarPorUsuario(selecionado.id));
    } catch (e) {
      setErro(mensagemDeErro(e, "Não foi possível carregar as reservas."));
      console.error(e);
    } finally {
      setCarregandoPainel(false);
    }
  };

  const carregarLogs = async (idDoUsuario) => {
    try {
      setCarregandoPainel(true);
      setErro(null);
      setLogs(
        idDoUsuario
          ? await logsApi.listarPorUsuario(idDoUsuario)
          : await logsApi.listar()
      );
    } catch (e) {
      setErro(mensagemDeErro(e, "Não foi possível carregar os logs."));
      console.error(e);
    } finally {
      setCarregandoPainel(false);
    }
  };

  const irParaLogs = async () => {
    abrirPagina("logs");
    setFiltroLogs(FILTRO_LOGS_VAZIO);
    await Promise.all([carregarLogs(""), carregarUsuariosGestao()]);
  };

  const carregarDashboard = async () => {
    try {
      setCarregandoPainel(true);
      setErro(null);
      setDashboard(await dashboardApi.obter());
    } catch (e) {
      setErro(mensagemDeErro(e, "Não foi possível carregar a dashboard."));
      console.error(e);
    } finally {
      setCarregandoPainel(false);
    }
  };

  const irParaDashboard = async () => {
    abrirPagina("dashboard");
    await carregarDashboard();
  };

  const atualizarConta = async (evento) => {
    evento.preventDefault();
    const nome = formConta.nome.trim();
    const email = formConta.email.trim();
    if (!nome || !email) {
      setErro("Informe o nome e o e-mail.");
      return;
    }

    try {
      setSalvando(true);
      setErro(null);
      const atualizado = await usuariosApi.atualizar(usuario.id, { nome, email });
      if (atualizado.email !== usuario.email) {
        encerrarSessao("E-mail alterado. Entre novamente com o novo e-mail.");
        setFormLogin({ email: atualizado.email, senha: "" });
        abrirAutenticacao("login");
        return;
      }
      const sessao = { ...usuario, nome: atualizado.nome };
      salvarSessao(sessao);
      setUsuario(sessao);
      setAviso("Seus dados foram atualizados.");
    } catch (e) {
      setErro(mensagemDeErro(e, "Não foi possível atualizar seus dados."));
      console.error(e);
    } finally {
      setSalvando(false);
    }
  };

  const iniciarTrocaDeSenha = async () => {
    setFormRecuperacao({ ...FORM_RECUPERACAO_VAZIO, email: usuario.email });
    setModoAutenticacao("recuperar");
    setAvisoAutenticacao(null);
    setMostrarAutenticacao(true);
    await enviarCodigoRecuperacao(usuario.email);
  };

  const excluirConta = () => {
    setErro(null);
    setDialogoExclusao(
      reservasAtivas.length > 0
        ? { bloqueado: true, mensagem: MENSAGEM_EXCLUSAO_BLOQUEADA }
        : { bloqueado: false }
    );
  };

  const confirmarExclusaoDaConta = async () => {
    try {
      setSalvando(true);
      setErro(null);
      await usuariosApi.deletar(usuario.id);
      setDialogoExclusao(null);
      encerrarSessao("Sua conta foi excluída.");
    } catch (e) {
      if (e && e.status === 409) {
        setDialogoExclusao({
          bloqueado: true,
          mensagem: mensagemDeErro(e, MENSAGEM_EXCLUSAO_BLOQUEADA),
        });
      } else {
        setDialogoExclusao(null);
        setErro(mensagemDeErro(e, "Não foi possível excluir a conta."));
      }
      console.error(e);
    } finally {
      setSalvando(false);
    }
  };

  const verReservasAntesDeExcluir = async () => {
    setDialogoExclusao(null);
    await irParaReservas();
  };

  const irParaRanking = async () => {
    abrirPagina("ranking");
    await carregarRanking();
  };

  const enviarAvaliacao = async (evento) => {
    evento.preventDefault();
    if (!usuarioId) {
      setErro("Entre na sua conta para avaliar livros.");
      abrirAutenticacao("login");
      return;
    }
    if (!formAvaliacao.nota) {
      setErro("Escolha uma nota de 1 a 5 estrelas.");
      return;
    }

    try {
      setSalvandoAvaliacao(true);
      setErro(null);
      await avaliacoesApi.avaliar(
        livroAbertoId,
        formAvaliacao.nota,
        formAvaliacao.comentario.trim() || null
      );
      setAviso(
        minhaAvaliacao ? "Sua avaliação foi atualizada." : "Obrigado pela avaliação!"
      );
      await carregarAvaliacoes(livroAbertoId);
    } catch (e) {
      setErro(mensagemDeErro(e, "Não foi possível registrar sua avaliação."));
      console.error(e);
    } finally {
      setSalvandoAvaliacao(false);
    }
  };

  const removerAvaliacao = async () => {
    if (!minhaAvaliacao || !window.confirm("Remover sua avaliação deste livro?")) {
      return;
    }

    try {
      setSalvandoAvaliacao(true);
      setErro(null);
      await avaliacoesApi.remover(minhaAvaliacao.id);
      setAviso("Sua avaliação foi removida.");
      await carregarAvaliacoes(livroAbertoId);
    } catch (e) {
      setErro(mensagemDeErro(e, "Não foi possível remover sua avaliação."));
      console.error(e);
    } finally {
      setSalvandoAvaliacao(false);
    }
  };

  const irParaMaterias = () => {
    abrirPagina("materias");
  };

  const irParaReservas = async () => {
    abrirPagina("reservas");
    if (ehEstudante) {
      await carregarReservas(usuarioId);
    }
  };

  const irParaCarrinho = async () => {
    abrirPagina("carrinho");
    if (ehEstudante) {
      await Promise.all([carregarCarrinho(), carregarPedidos()]);
    }
  };

  const irParaPedidos = async () => {
    abrirPagina("pedidos");
    setRetornoPagamento(null);
    if (ehEstudante) {
      await carregarPedidos();
    }
  };

  const adicionarAoCarrinho = async (livro) => {
    if (!usuarioId) {
      abrirAutenticacao("login");
      return;
    }
    if (itemCarrinhoEmEdicao === livro.id) {
      return;
    }

    const itemExistente = carrinho.itens.find(
      (item) => item.livroId === livro.id
    );

    try {
      setItemCarrinhoEmEdicao(livro.id);
      setErro(null);
      setCarrinho(
        await carrinhoApi.definirQuantidade(
          livro.id,
          (itemExistente ? itemExistente.quantidade : 0) + 1
        )
      );
      setAviso('"' + livro.titulo + '" foi adicionado ao carrinho.');
    } catch (e) {
      setErro(mensagemDeErro(e, "Não foi possível adicionar ao carrinho."));
      console.error(e);
    } finally {
      setItemCarrinhoEmEdicao(null);
    }
  };

  const alterarQuantidadeNoCarrinho = async (item, quantidade) => {
    try {
      setItemCarrinhoEmEdicao(item.livroId);
      setErro(null);
      setCarrinho(
        await carrinhoApi.definirQuantidade(item.livroId, quantidade)
      );
    } catch (e) {
      setErro(mensagemDeErro(e, "Não foi possível alterar a quantidade."));
      console.error(e);
    } finally {
      setItemCarrinhoEmEdicao(null);
    }
  };

  const removerDoCarrinho = async (item) => {
    try {
      setItemCarrinhoEmEdicao(item.livroId);
      setErro(null);
      setCarrinho(await carrinhoApi.remover(item.livroId));
    } catch (e) {
      setErro(mensagemDeErro(e, "Não foi possível remover o item."));
      console.error(e);
    } finally {
      setItemCarrinhoEmEdicao(null);
    }
  };

  const irParaMercadoPago = async (link) => {
    try {
      const resposta = await usuariosApi.renovarToken();
      const sessao = {
        ...usuario,
        nome: resposta.usuario.nome,
        token: resposta.token,
      };
      const agora = Date.now();
      ultimaAtividade.current = agora;
      salvarUltimaAtividade(agora);
      salvarSessao(sessao);
      definirToken(sessao.token);
      setUsuario(sessao);
    } catch (e) {
      console.error(e);
    }
    window.location.assign(link);
  };

  const finalizarCompra = async () => {
    if (finalizandoCompra) {
      return;
    }

    try {
      setFinalizandoCompra(true);
      setErro(null);
      if (pedidoAguardando && pedidoAguardando.linkPagamento) {
        await irParaMercadoPago(pedidoAguardando.linkPagamento);
        return;
      }
      const pedido = await pedidosApi.finalizarCompra();
      await irParaMercadoPago(pedido.linkPagamento);
    } catch (e) {
      console.error(e);
      await Promise.all([
        carregarLivros(),
        carregarCarrinho(),
        carregarPedidos(),
      ]);
      setErro(mensagemDeErro(e, "Não foi possível finalizar a compra."));
    } finally {
      setFinalizandoCompra(false);
    }
  };

  const alternarFavorito = async (livro) => {
    if (!usuarioId) {
      setErro("Entre na sua conta para favoritar livros.");
      abrirAutenticacao("login");
      return;
    }
    if (favoritoEmEdicao === livro.id) {
      return;
    }

    try {
      setFavoritoEmEdicao(livro.id);
      setErro(null);

      if (idsFavoritos.has(livro.id)) {
        await usuariosApi.desfavoritar(usuarioId, livro.id);
      } else {
        await usuariosApi.favoritar(usuarioId, livro.id);
      }

      setFavoritos(await usuariosApi.listarFavoritos(usuarioId));
    } catch (e) {
      setErro(mensagemDeErro(e, "Não foi possível atualizar os favoritos."));
      console.error(e);
    } finally {
      setFavoritoEmEdicao(null);
    }
  };

  const reservaAtivaDoLivro = useCallback(
    (livro) =>
      reservas.find(
        (reserva) =>
          ["RESERVADO", "ENTREGUE"].includes(reserva.status) &&
          reserva.livroId === livro.id
      ) || null,
    [reservas]
  );

  const reservarLivro = async (livro) => {
    if (!usuarioId) {
      setErro("Entre na sua conta para reservar livros.");
      abrirAutenticacao("login");
      return;
    }
    if (Number(livro.estoqueReserva || 0) <= 0) {
      setErro("Este livro está sem estoque no momento.");
      return;
    }

    try {
      setReservaEmEdicao(livro.id);
      setErro(null);
      await reservasApi.reservar(usuarioId, livro.id);
      setAviso('"' + livro.titulo + '" foi reservado.');
      await Promise.all([carregarLivros(), carregarReservas(usuarioId)]);
    } catch (e) {
      setErro(
        mensagemDeErro(e, "Não foi possível reservar o livro. Ele pode estar esgotado.")
      );
      console.error(e);
    } finally {
      setReservaEmEdicao(null);
    }
  };

  const cancelarReserva = async (reserva) => {
    try {
      setReservaEmEdicao(reserva.id);
      setErro(null);
      await reservasApi.cancelar(reserva.id);
      setAviso("Reserva de \"" + reserva.tituloLivro + "\" cancelada.");
      await Promise.all([carregarLivros(), carregarReservas(usuarioId)]);
    } catch (e) {
      setErro(mensagemDeErro(e, "Não foi possível cancelar a reserva."));
      console.error(e);
    } finally {
      setReservaEmEdicao(null);
    }
  };

  const atualizarReservaNaGestao = async (reserva, acao, sucesso, falha) => {
    try {
      setReservaEmEdicao(reserva.id);
      setErro(null);
      await acao(reserva.id);
      setAviso("\"" + reserva.tituloLivro + "\" " + sucesso);
      const [, atualizadas] = await Promise.all([
        carregarLivros(),
        reservasApi.listarPorUsuario(usuarioGestao.id),
      ]);
      setReservasGestao(atualizadas);
    } catch (e) {
      setErro(mensagemDeErro(e, falha));
      console.error(e);
    } finally {
      setReservaEmEdicao(null);
    }
  };

  const entregarLivro = (reserva) =>
    atualizarReservaNaGestao(
      reserva,
      reservasApi.entregar,
      "foi entregue.",
      "Não foi possível registrar a entrega."
    );

  const devolverLivro = (reserva) =>
    atualizarReservaNaGestao(
      reserva,
      reservasApi.devolver,
      "foi devolvido.",
      "Não foi possível registrar a devolução."
    );

  const pesquisar = async (evento) => {
    evento.preventDefault();

    if (!busca.trim()) {
      limparFiltro();
      return;
    }

    try {
      setCarregando(true);
      setErro(null);
      const livro = await livrosApi.buscarPorTitulo(busca.trim());
      setResultado(livro ? [livro] : []);
      setFiltro({ tipo: "busca", termo: busca.trim() });
    } catch (e) {
      setResultado([]);
      setFiltro({ tipo: "busca", termo: busca.trim() });
      setErro("Nenhum livro encontrado para: " + busca);
      console.error(e);
    } finally {
      setCarregando(false);
    }
  };

  const filtrarPorMateria = async (materia) => {
    try {
      setPagina("inicio");
      setLivroAbertoId(null);
      setFormEdicao(null);
      setCarregando(true);
      setErro(null);
      setBusca("");
      setResultado(await livrosApi.filtrarPorMateria(materia.nome));
      setFiltro({ tipo: "materia", nome: materia.nome, rotulo: materia.rotulo });
    } catch (e) {
      setResultado([]);
      setErro("Não foi possível filtrar por " + materia.rotulo + ".");
      console.error(e);
    } finally {
      setCarregando(false);
    }
  };

  const cadastrarLivro = async (evento) => {
    evento.preventDefault();
    if (!formLivro.titulo.trim() || !formLivro.materia) {
      setErro("Informe o título e a matéria do livro.");
      return;
    }

    try {
      setSalvando(true);
      setErro(null);
      const livro = await livrosApi.cadastrar(
        formLivro.titulo.trim(),
        formLivro.materia,
        Number(String(formLivro.preco).replace(",", ".")) || 0,
        Number(formLivro.estoqueReserva) || 0,
        Number(formLivro.estoqueCompra) || 0
      );
      setFormLivro(FORM_LIVRO_VAZIO);
      setMostrarCadastroLivro(false);
      setAviso('"' + livro.titulo + '" foi cadastrado.');
      await Promise.all([carregarLivros(), carregarMaterias()]);
      limparFiltro();
    } catch (e) {
      setErro(
        mensagemDeErro(
          e,
          "Não foi possível cadastrar o livro. Confira o título na API de livros."
        )
      );
      console.error(e);
    } finally {
      setSalvando(false);
    }
  };

  const atualizarLivro = async (evento) => {
    evento.preventDefault();

    try {
      setSalvando(true);
      setErro(null);
      const atualizado = await livrosApi.atualizar(formEdicao.id, {
        titulo: formEdicao.titulo,
        anoLancamento: formEdicao.anoLancamento,
        numeroPagina: Number(formEdicao.numeroPagina) || 0,
        materia: nomeDaMateria(formEdicao.materia) || null,
        preco: Number(String(formEdicao.preco).replace(",", ".")) || 0,
        estoqueReserva: Number(formEdicao.estoqueReserva) || 0,
        estoqueCompra: Number(formEdicao.estoqueCompra) || 0,
        isbn10: formEdicao.isbn10 || "",
        isbn13: formEdicao.isbn13 || "",
      });
      setAviso('"' + atualizado.titulo + '" foi atualizado.');
      setFormEdicao({ ...atualizado });
      setEditandoLivro(false);
      setResultado((atual) =>
        atual.map((item) => (item.id === atualizado.id ? atualizado : item))
      );
      await Promise.all([carregarLivros(), carregarMaterias()]);
    } catch (e) {
      setErro(mensagemDeErro(e, "Não foi possível atualizar o livro."));
      console.error(e);
    } finally {
      setSalvando(false);
    }
  };

  const alterarEdicao = (campo, valor) =>
    setFormEdicao((atual) => ({ ...atual, [campo]: valor }));

  const iniciarEdicaoDoLivro = (livro) => {
    setFormEdicao({ ...livro });
    setErro(null);
    setEditandoLivro(true);
  };

  const cancelarEdicaoDoLivro = (livro) => {
    setFormEdicao({ ...livro });
    setErro(null);
    setEditandoLivro(false);
  };

  const deletarLivro = async (livro) => {
    if (!window.confirm('Remover "' + livro.titulo + '" do acervo?')) {
      return;
    }

    try {
      setSalvando(true);
      setErro(null);

      try {
        await livrosApi.deletar(livro.id, false);
      } catch (conflito) {
        if (conflito.status !== 409) {
          throw conflito;
        }
        const seguir = window.confirm(
          'O livro "' +
            livro.titulo +
            '" está reservado neste momento. Excluir agora também apaga as reservas ' +
            "dele. Tem certeza de que deseja continuar?"
        );
        if (!seguir) {
          return;
        }
        await livrosApi.deletar(livro.id, true);
      }

      setLivroAbertoId(null);
      setFormEdicao(null);
      setCapaAmpliada(null);
      setAviso('"' + livro.titulo + '" foi removido do acervo.');
      await Promise.all([carregarLivros(), carregarMaterias()]);
      limparFiltro();
    } catch (e) {
      setErro(e.message || "Não foi possível remover o livro.");
      console.error(e);
    } finally {
      setSalvando(false);
    }
  };

  const cadastrarUsuario = async (evento) => {
    evento.preventDefault();
    const email = formUsuario.email.trim();

    if (!REGRA_SENHA.test(formUsuario.senha)) {
      setErro(DICA_SENHA);
      return;
    }
    if (formUsuario.senha !== formUsuario.confirmarSenha) {
      setErro("As senhas não conferem.");
      return;
    }
    if (formUsuario.tipo === "FUNCIONARIO" && !formUsuario.codigoAcesso.trim()) {
      setErro("Informe o código de acesso de funcionário.");
      return;
    }
    if (!formUsuario.aceitouTermos) {
      setErro(
        "Para criar a conta, aceite os Termos de Uso e a Política de Privacidade."
      );
      return;
    }

    try {
      setSalvando(true);
      setErro(null);
      let versaoTermos = null;
      try {
        versaoTermos = await carregarVersaoDosTermos();
      } catch (e) {
        console.error(e);
      }
      await usuariosApi.cadastrar({
        nome: formUsuario.nome.trim(),
        email,
        senha: formUsuario.senha,
        tipo: formUsuario.tipo,
        codigoAcesso:
          formUsuario.tipo === "FUNCIONARIO"
            ? formUsuario.codigoAcesso.trim()
            : null,
        aceitouTermos: formUsuario.aceitouTermos,
        versaoTermos,
      });
      setFormUsuario(FORM_USUARIO_VAZIO);
      setFormLogin({ email, senha: "" });
      setModoAutenticacao("login");
      setAvisoAutenticacao("Conta criada! Entre com seu e-mail e senha.");
    } catch (e) {
      setErro(mensagemDeErro(e, "Não foi possível criar a conta."));
      console.error(e);
    } finally {
      setSalvando(false);
    }
  };

  const livrosExibidos = filtro.tipo === "todos" ? livros : resultado;
  const totalPaginasLivros = Math.max(
    1,
    Math.ceil(livrosExibidos.length / LIVROS_POR_PAGINA)
  );
  const paginaLivrosAtual = Math.min(paginaLivros, totalPaginasLivros);
  const livrosDaPagina = livrosExibidos.slice(
    (paginaLivrosAtual - 1) * LIVROS_POR_PAGINA,
    paginaLivrosAtual * LIVROS_POR_PAGINA
  );

  const irParaPaginaDeLivros = (numero) => {
    setPaginaLivros(numero);
    const secao = document.getElementById("livros");
    if (secao && secao.scrollIntoView) {
      secao.scrollIntoView({ behavior: "smooth", block: "start" });
    }
  };

  const tituloDaLista =
    filtro.tipo === "busca"
      ? 'Resultado para "' + filtro.termo + '"'
      : filtro.tipo === "materia"
      ? "Livros de " + filtro.rotulo
      : "Livros em destaque";

  const rotuloDaLista =
    filtro.tipo === "todos" ? "RECOMENDADOS" : "RESULTADO DA BUSCA";

  const rankingDeLivros = useMemo(
    () =>
      ranking.map((item) => ({
        ...(livros.find((livro) => livro.id === item.id) || {}),
        ...item,
      })),
    [ranking, livros]
  );

  const usuariosFiltrados = useMemo(() => {
    const termo = filtroUsuarios.trim().toLowerCase();
    return usuariosGestao
      .filter((item) => item.tipo === "ESTUDANTE")
      .filter(
        (item) =>
          !termo ||
          String(item.nome || "").toLowerCase().includes(termo) ||
          String(item.email || "").toLowerCase().includes(termo)
      );
  }, [usuariosGestao, filtroUsuarios]);

  const acoesDosLogs = useMemo(
    () => [...new Set(logs.map((log) => log.acao).filter(Boolean))].sort(),
    [logs]
  );

  const periodoDosLogsInvalido = Boolean(
    filtroLogs.de && filtroLogs.ate && filtroLogs.de > filtroLogs.ate
  );

  const logsExibidos = useMemo(() => {
    const termo = filtroLogs.texto.trim().toLowerCase();
    return logs.filter((log) => {
      const dia = String(log.dataHora || "").slice(0, 10);
      if (filtroLogs.acao && log.acao !== filtroLogs.acao) {
        return false;
      }
      if (filtroLogs.de && dia < filtroLogs.de) {
        return false;
      }
      if (filtroLogs.ate && dia > filtroLogs.ate) {
        return false;
      }
      return (
        !termo ||
        [log.acao, log.descricao, log.usuarioEmail]
          .map((valor) => String(valor || "").toLowerCase())
          .some((valor) => valor.includes(termo))
      );
    });
  }, [logs, filtroLogs.texto, filtroLogs.acao, filtroLogs.de, filtroLogs.ate]);

  const filtrosDeLogsAtivos = Boolean(
    filtroLogs.texto || filtroLogs.acao || filtroLogs.de || filtroLogs.ate
  );

  const reservasAtivas = useMemo(
    () =>
      reservas.filter((reserva) =>
        ["RESERVADO", "ENTREGUE"].includes(reserva.status)
      ),
    [reservas]
  );

  const quantidadeNoCarrinho = carrinho.itens.reduce(
    (total, item) => total + item.quantidade,
    0
  );

  const mensagemListaVazia =
    filtro.tipo === "todos"
      ? "Nenhum livro cadastrado ainda."
      : "Nenhum livro encontrado para este filtro.";

  const livroAberto =
    livroAbertoId === null
      ? null
      : [...livros, ...favoritos, ...resultado].find(
          (livro) => livro.id === livroAbertoId
        ) || formEdicao;

  const abrirCadastroLivro = () => {
    setErro(null);
    setMostrarCadastroLivro(true);
  };

  const cartaoLivro = (livro) => (
    <div
      className="book-card"
      key={livro.id}
      role="button"
      tabIndex={0}
      onClick={() => abrirLivro(livro)}
      onKeyDown={(e) => {
        if (e.key === "Enter") {
          abrirLivro(livro);
        }
      }}
    >
      <div className="book-cover">
        {livro.capaUrl ? (
          <img
            src={capaNaResolucao(livro.capaUrl, ZOOM_CAPA_CARTAO)}
            alt={"Capa de " + livro.titulo}
            loading="lazy"
          />
        ) : (
          <CapaGenerica titulo={livro.titulo} tamanho="cartao" />
        )}
        {!ehFuncionario && (
          <button
            type="button"
            className={"favorite" + (idsFavoritos.has(livro.id) ? " ativo" : "")}
            onClick={(e) => {
              e.stopPropagation();
              alternarFavorito(livro);
            }}
            disabled={favoritoEmEdicao === livro.id}
            aria-label={
              idsFavoritos.has(livro.id)
                ? "Remover dos favoritos"
                : "Adicionar aos favoritos"
            }
          >
            {idsFavoritos.has(livro.id) ? "♥" : "♡"}
          </button>
        )}
      </div>
      <div className="book-info">
        <span className="book-category">{livro.materia}</span>
        <h3>{livro.titulo}</h3>
        <p>{(livro.autores || []).join(", ")}</p>
        <div className="book-footer">
          <strong>{formatarPreco(livro.preco)}</strong>
          <span
            className={
              "estoque" + (Number(livro.estoqueReserva || 0) <= 0 ? " esgotado" : "")
            }
          >
            {formatarEstoque(livro.estoqueReserva)}
          </span>
        </div>
        {!ehFuncionario && (
          <button
            type="button"
            className="reservar-button"
            onClick={(e) => {
              e.stopPropagation();
              reservarLivro(livro);
            }}
            disabled={
              reservaEmEdicao === livro.id || Number(livro.estoqueReserva || 0) <= 0
            }
          >
            {Number(livro.estoqueReserva || 0) <= 0 ? "Indisponível" : "Reservar"}
          </button>
        )}
        {!ehFuncionario && (
          <button
            type="button"
            className="reservar-button"
            onClick={(e) => {
              e.stopPropagation();
              adicionarAoCarrinho(livro);
            }}
            disabled={
              itemCarrinhoEmEdicao === livro.id || !podeSerComprado(livro)
            }
          >
            {podeSerComprado(livro)
              ? "Adicionar ao carrinho"
              : "Indisponível para compra"}
          </button>
        )}
      </div>
    </div>
  );

  useEffect(() => {
    setPaginaLivros(1);
  }, [filtro]);

  const idUsuarioLogado = usuario ? usuario.id : null;

  useEffect(() => {
    setTermosPendentes(null);
    setDataAceiteTermos(null);
    if (!idUsuarioLogado) {
      return undefined;
    }
    let ativo = true;
    Promise.all([carregarVersaoDosTermos(), usuariosApi.statusTermos()])
      .then(([versao, status]) => {
        if (ativo && status) {
          setDataAceiteTermos(status.dataAceite || null);
        }
        if (ativo && versao && (!status || status.versaoAceita !== versao)) {
          setTermosLidos({ termos: false, privacidade: false });
          setAceiteTermos(false);
          setErroTermos(null);
          setTermosPendentes({ versao });
        }
      })
      .catch((e) => console.error(e));
    return () => {
      ativo = false;
    };
  }, [idUsuarioLogado]);

  useEffect(() => {
    if (termosPendentes && documentoAberto) {
      setTermosLidos((lidos) => ({ ...lidos, [documentoAberto]: true }));
    }
  }, [termosPendentes, documentoAberto]);

  const aceitarTermosAtualizados = async () => {
    try {
      setSalvando(true);
      setErroTermos(null);
      const aceite = await usuariosApi.aceitarTermos(termosPendentes.versao);
      setDataAceiteTermos(aceite ? aceite.dataAceite : null);
      setTermosPendentes(null);
      setAviso("Obrigado! Você aceitou a nova versão dos termos.");
    } catch (e) {
      setErroTermos(
        mensagemDeErro(e, "Não foi possível registrar o aceite. Tente novamente.")
      );
      console.error(e);
    } finally {
      setSalvando(false);
    }
  };

  useEffect(() => {
    if (!menuAberto) {
      return undefined;
    }
    const aoClicarFora = (evento) => {
      if (menuRef.current && !menuRef.current.contains(evento.target)) {
        setMenuAberto(false);
      }
    };
    const aoTeclar = (evento) => {
      if (evento.key === "Escape") {
        setMenuAberto(false);
      }
    };
    document.addEventListener("mousedown", aoClicarFora);
    document.addEventListener("keydown", aoTeclar);
    return () => {
      document.removeEventListener("mousedown", aoClicarFora);
      document.removeEventListener("keydown", aoTeclar);
    };
  }, [menuAberto]);

  useEffect(() => {
    if (!dialogoExclusao) {
      return undefined;
    }
    const aoTeclar = (evento) => {
      if (evento.key === "Escape" && !salvando) {
        setDialogoExclusao(null);
      }
    };
    document.addEventListener("keydown", aoTeclar);
    return () => document.removeEventListener("keydown", aoTeclar);
  }, [dialogoExclusao, salvando]);

  const cabecalho = (
    <header className="header">
      <button type="button" className="logo" onClick={irParaInicio}>
        Pallanthir
      </button>
      <div className="header-actions">
        {!ehFuncionario && (
          <button
            type="button"
            className="carrinho-atalho"
            onClick={irParaCarrinho}
            aria-label={
              "Carrinho" +
              (quantidadeNoCarrinho > 0 ? " (" + quantidadeNoCarrinho + ")" : "")
            }
          >
            🛒 {quantidadeNoCarrinho > 0 && quantidadeNoCarrinho}
          </button>
        )}
        {usuario ? (
          <div className="sessao">
            <button
              type="button"
              className={"sessao-nome" + (pagina === "conta" ? " ativo" : "")}
              onClick={irParaConta}
              title="Minha conta"
            >
              {usuario.nome}
              <small>{ROTULO_TIPO_USUARIO[usuario.tipo] || ""}</small>
            </button>
          </div>
        ) : (
          <button
            type="button"
            className="login-button"
            onClick={() => abrirAutenticacao("login")}
          >
            Entrar
          </button>
        )}
        <div className="menu" ref={menuRef}>
          <button
            type="button"
            className={"menu-botao" + (menuAberto ? " aberto" : "")}
            onClick={() => setMenuAberto((aberto) => !aberto)}
            aria-label={menuAberto ? "Fechar menu" : "Abrir menu"}
            aria-expanded={menuAberto}
            aria-controls="menu-navegacao"
          >
            <span />
            <span />
            <span />
          </button>
          <nav
            id="menu-navegacao"
            className={"menu-painel" + (menuAberto ? " aberto" : "")}
            onClick={() => setMenuAberto(false)}
          >
            <button
              type="button"
              className={
                "nav-link" +
                (pagina === "inicio" && filtro.tipo === "todos" ? " active" : "")
              }
              onClick={irParaInicio}
            >
              Início
            </button>
            <button
              type="button"
              className={"nav-link" + (pagina === "materias" ? " active" : "")}
              onClick={irParaMaterias}
            >
              Matérias
            </button>
            <button
              type="button"
              className={"nav-link" + (pagina === "ranking" ? " active" : "")}
              onClick={irParaRanking}
            >
              Ranking
            </button>
            {!ehFuncionario && (
              <button
                type="button"
                className={
                  "nav-link" + (pagina === "favoritos" ? " active" : "")
                }
                onClick={irParaFavoritos}
              >
                Favoritos ♡ {favoritos.length > 0 && "(" + favoritos.length + ")"}
              </button>
            )}
            {!ehFuncionario && (
              <button
                type="button"
                className={"nav-link" + (pagina === "reservas" ? " active" : "")}
                onClick={irParaReservas}
              >
                Reservas{" "}
                {reservasAtivas.length > 0 && "(" + reservasAtivas.length + ")"}
              </button>
            )}
            {!ehFuncionario && (
              <button
                type="button"
                className={"nav-link" + (pagina === "carrinho" ? " active" : "")}
                onClick={irParaCarrinho}
              >
                Carrinho{" "}
                <svg
                  className="icone-menu"
                  viewBox="0 0 24 24"
                  width="15"
                  height="15"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  aria-hidden="true"
                >
                  <circle cx="9" cy="20" r="1.4" />
                  <circle cx="18" cy="20" r="1.4" />
                  <path d="M2.5 3h2.6l2.4 11.2a1.5 1.5 0 0 0 1.5 1.2h8.9a1.5 1.5 0 0 0 1.5-1.1L21.5 7H6" />
                </svg>{" "}
                {quantidadeNoCarrinho > 0 && "(" + quantidadeNoCarrinho + ")"}
              </button>
            )}
            {!ehFuncionario && (
              <button
                type="button"
                className={"nav-link" + (pagina === "pedidos" ? " active" : "")}
                onClick={irParaPedidos}
              >
                Pedidos
              </button>
            )}
            {ehFuncionario && (
              <button
                type="button"
                className={"nav-link" + (pagina === "dashboard" ? " active" : "")}
                onClick={irParaDashboard}
              >
                Dashboard
              </button>
            )}
            {ehFuncionario && (
              <button
                type="button"
                className={"nav-link" + (pagina === "gestao" ? " active" : "")}
                onClick={irParaGestao}
              >
                Gestão
              </button>
            )}
            {ehFuncionario && (
              <button
                type="button"
                className={"nav-link" + (pagina === "logs" ? " active" : "")}
                onClick={irParaLogs}
              >
                Logs
              </button>
            )}
          </nav>
        </div>
      </div>
    </header>
  );

  const modalCadastroLivro = mostrarCadastroLivro && (
    <div className="modal" onClick={() => setMostrarCadastroLivro(false)}>
      <div className="modal-box" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3>Cadastrar livro</h3>
          <button type="button" onClick={() => setMostrarCadastroLivro(false)}>
            ✕
          </button>
        </div>
        <p className="modal-sub">
          Os dados do livro são buscados pelo título na API de livros do Google.
        </p>

        <form className="modal-form" onSubmit={cadastrarLivro}>
          <label>
            Título
            <input
              type="text"
              placeholder="Título do livro"
              value={formLivro.titulo}
              onChange={(e) =>
                setFormLivro({ ...formLivro, titulo: e.target.value })
              }
            />
          </label>
          <label>
            Matéria
            <select
              value={formLivro.materia}
              onChange={(e) =>
                setFormLivro({ ...formLivro, materia: e.target.value })
              }
            >
              <option value="">Selecione a matéria</option>
              {materias.map((materia) => (
                <option key={materia.nome} value={materia.nome}>
                  {materia.rotulo}
                </option>
              ))}
            </select>
          </label>
          <label>
            Preço
            <input
              type="number"
              min="0"
              step="0.01"
              placeholder="0.00"
              value={formLivro.preco}
              onChange={(e) =>
                setFormLivro({ ...formLivro, preco: e.target.value })
              }
            />
          </label>
          <label>
            Estoque para reserva
            <input
              type="number"
              min="0"
              step="1"
              placeholder="Quantidade disponível para reserva"
              value={formLivro.estoqueReserva}
              onChange={(e) =>
                setFormLivro({ ...formLivro, estoqueReserva: e.target.value })
              }
            />
          </label>
          <label>
            Estoque para compra
            <input
              type="number"
              min="0"
              step="1"
              placeholder="Quantidade disponível para venda"
              value={formLivro.estoqueCompra}
              onChange={(e) =>
                setFormLivro({ ...formLivro, estoqueCompra: e.target.value })
              }
            />
          </label>

          {erro && <p className="erro">{erro}</p>}

          <div className="modal-actions">
            <button type="submit" disabled={salvando}>
              {salvando ? "Salvando..." : "Cadastrar"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );

  const subtituloAutenticacao = {
    login:
      "Informe seu e-mail e senha. Depois enviaremos um código de confirmação para o seu e-mail.",
    cadastro: "Crie sua conta de estudante ou de funcionário da biblioteca.",
    "2fa":
      "Digite o código de 6 dígitos enviado para " +
      emailPendente +
      ". Ele vale por 10 minutos.",
    recuperar:
      "Informe o e-mail da sua conta para receber um código de recuperação.",
    redefinir:
      "Digite o código enviado para " +
      formRecuperacao.email +
      " e escolha uma nova senha. O código vale por 15 minutos.",
  }[modoAutenticacao];

  const modalExclusaoConta = dialogoExclusao && (
    <div
      className="modal"
      onClick={() => !salvando && setDialogoExclusao(null)}
    >
      <div
        className={
          "modal-box dialogo" + (dialogoExclusao.bloqueado ? " bloqueado" : " perigo")
        }
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="dialogo-exclusao-titulo"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="dialogo-icone" aria-hidden="true">
          {dialogoExclusao.bloqueado ? "!" : "✕"}
        </div>
        <h3 id="dialogo-exclusao-titulo">
          {dialogoExclusao.bloqueado
            ? "Não é possível excluir sua conta"
            : "Excluir sua conta?"}
        </h3>
        {dialogoExclusao.bloqueado ? (
          <p>{dialogoExclusao.mensagem}</p>
        ) : (
          <>
            <p>
              Seus dados pessoais serão anonimizados e você perderá o acesso aos
              seus favoritos, reservas e pedidos.
            </p>
            <p className="dialogo-alerta">Esta ação não pode ser desfeita.</p>
          </>
        )}
        <div className="dialogo-acoes">
          {dialogoExclusao.bloqueado ? (
            <>
              <button
                type="button"
                className="dialogo-secundario"
                onClick={() => setDialogoExclusao(null)}
              >
                Entendi
              </button>
              <button
                type="button"
                className="dialogo-principal"
                onClick={verReservasAntesDeExcluir}
              >
                Ver minhas reservas
              </button>
            </>
          ) : (
            <>
              <button
                type="button"
                className="dialogo-secundario"
                onClick={() => setDialogoExclusao(null)}
                disabled={salvando}
              >
                Cancelar
              </button>
              <button
                type="button"
                className="dialogo-principal"
                onClick={confirmarExclusaoDaConta}
                disabled={salvando}
              >
                {salvando ? "Excluindo..." : "Excluir conta"}
              </button>
            </>
          )}
        </div>
      </div>
    </div>
  );

  const modalDocumento = documentoAberto && (
    <ModalDocumento
      documento={documentoAberto}
      aoFechar={() => setDocumentoAberto(null)}
      aoTrocar={setDocumentoAberto}
    />
  );

  const modalAutenticacao = mostrarAutenticacao && (
    <div className="modal modal-auth">
      <div className="modal-box auth-box">
        <div className="auth-conteudo">
        <button
          type="button"
          className="auth-fechar"
          onClick={fecharAutenticacao}
          aria-label="Fechar"
        >
          ✕
        </button>
        <div className="auth-cabecalho">
          <span className="auth-logo" aria-hidden="true">
            Pallanthir
          </span>
          <h3 className="oculto-visualmente">
            {TITULOS_AUTENTICACAO[modoAutenticacao]}
          </h3>
          <p className="modal-sub">{subtituloAutenticacao}</p>
        </div>

        {avisoAutenticacao && (
          <p className="aviso-modal">{avisoAutenticacao}</p>
        )}

        {modoAutenticacao === "login" && (
          <form className="modal-form" onSubmit={entrar}>
            <label>
              E-mail
              <input
                type="email"
                required
                value={formLogin.email}
                onChange={(e) =>
                  setFormLogin({ ...formLogin, email: e.target.value })
                }
              />
            </label>
            <CampoSenha
              rotulo="Senha"
              autoComplete="current-password"
              valor={formLogin.senha}
              aoMudar={(senha) => setFormLogin({ ...formLogin, senha })}
            />
            <button
              type="button"
              className="link-button auth-esqueci"
              onClick={() => {
                setFormRecuperacao({
                  ...FORM_RECUPERACAO_VAZIO,
                  email: formLogin.email.trim(),
                });
                trocarModoAutenticacao("recuperar");
              }}
            >
              Esqueci minha senha
            </button>

            {erro && <p className="erro">{erro}</p>}

            <div className="modal-actions">
              <button type="submit" disabled={salvando}>
                {salvando ? "Enviando código..." : "Entrar"}
              </button>
            </div>
            <p className="auth-alternar">
              Ainda não tem conta?{" "}
              <button
                type="button"
                className="link-button"
                onClick={() => trocarModoAutenticacao("cadastro")}
              >
                Criar conta
              </button>
            </p>
          </form>
        )}

        {modoAutenticacao === "2fa" && (
          <form className="modal-form" onSubmit={confirmarCodigo2FA}>
            <label>
              Código de verificação
              <input
                type="text"
                inputMode="numeric"
                autoComplete="one-time-code"
                maxLength={6}
                required
                className="campo-codigo"
                placeholder="000000"
                value={codigo2FA}
                onChange={(e) =>
                  setCodigo2FA(e.target.value.replace(/\D/g, ""))
                }
              />
            </label>

            {erro && <p className="erro">{erro}</p>}

            <div className="modal-actions">
              <button type="submit" disabled={salvando || codigo2FA.length < 6}>
                {salvando ? "Verificando..." : "Confirmar"}
              </button>
            </div>
            <div className="links-autenticacao">
              <button
                type="button"
                className="link-button"
                onClick={reenviarCodigo2FA}
                disabled={salvando}
              >
                Reenviar código
              </button>
              <button
                type="button"
                className="link-button"
                onClick={() => trocarModoAutenticacao("login")}
              >
                Voltar
              </button>
            </div>
          </form>
        )}

        {modoAutenticacao === "recuperar" && (
          <form className="modal-form" onSubmit={solicitarRecuperacao}>
            <label>
              E-mail
              <input
                type="email"
                required
                value={formRecuperacao.email}
                onChange={(e) =>
                  setFormRecuperacao({
                    ...formRecuperacao,
                    email: e.target.value,
                  })
                }
              />
            </label>

            {erro && <p className="erro">{erro}</p>}

            <div className="modal-actions">
              <button type="submit" disabled={salvando}>
                {salvando ? "Enviando..." : "Enviar código"}
              </button>
            </div>
            {!usuario && (
              <div className="links-autenticacao">
                <button
                  type="button"
                  className="link-button"
                  onClick={() => trocarModoAutenticacao("login")}
                >
                  Voltar para o login
                </button>
              </div>
            )}
          </form>
        )}

        {modoAutenticacao === "redefinir" && (
          <form className="modal-form" onSubmit={redefinirSenha}>
            <label>
              Código de recuperação
              <input
                type="text"
                inputMode="numeric"
                maxLength={6}
                required
                className="campo-codigo"
                placeholder="000000"
                value={formRecuperacao.codigo}
                onChange={(e) =>
                  setFormRecuperacao({
                    ...formRecuperacao,
                    codigo: e.target.value.replace(/\D/g, ""),
                  })
                }
              />
            </label>
            <CampoSenha
              rotulo="Nova senha"
              autoComplete="new-password"
              valor={formRecuperacao.novaSenha}
              aoMudar={(novaSenha) =>
                setFormRecuperacao({ ...formRecuperacao, novaSenha })
              }
            />
            <small className="dica-senha">{DICA_SENHA}</small>
            <CampoSenha
              rotulo="Confirmar nova senha"
              autoComplete="new-password"
              valor={formRecuperacao.confirmarSenha}
              aoMudar={(confirmarSenha) =>
                setFormRecuperacao({ ...formRecuperacao, confirmarSenha })
              }
            />

            {erro && <p className="erro">{erro}</p>}

            <div className="modal-actions">
              <button type="submit" disabled={salvando}>
                {salvando ? "Salvando..." : "Redefinir senha"}
              </button>
            </div>
            <div className="links-autenticacao">
              <button
                type="button"
                className="link-button"
                onClick={() => enviarCodigoRecuperacao(formRecuperacao.email)}
                disabled={salvando}
              >
                Reenviar código
              </button>
            </div>
          </form>
        )}

        {modoAutenticacao === "cadastro" && (
          <form className="modal-form" onSubmit={cadastrarUsuario}>
            <label>
              Nome
              <input
                type="text"
                required
                value={formUsuario.nome}
                onChange={(e) =>
                  setFormUsuario({ ...formUsuario, nome: e.target.value })
                }
              />
            </label>
            <label>
              E-mail
              <input
                type="email"
                required
                value={formUsuario.email}
                onChange={(e) =>
                  setFormUsuario({ ...formUsuario, email: e.target.value })
                }
              />
            </label>
            <div className="auth-linha">
              <CampoSenha
                rotulo="Senha"
                autoComplete="new-password"
                valor={formUsuario.senha}
                aoMudar={(senha) => setFormUsuario({ ...formUsuario, senha })}
              />
              <CampoSenha
                rotulo="Confirmar senha"
                autoComplete="new-password"
                valor={formUsuario.confirmarSenha}
                aoMudar={(confirmarSenha) =>
                  setFormUsuario({ ...formUsuario, confirmarSenha })
                }
              />
            </div>
            <RegrasDaSenha
              senha={formUsuario.senha}
              confirmacao={formUsuario.confirmarSenha}
            />
            <div className="auth-campo">
              <span className="auth-rotulo">Tipo de conta</span>
              <div
                className="tipo-conta"
                role="radiogroup"
                aria-label="Tipo de conta"
              >
                {[
                  ["ESTUDANTE", "Estudante", "Reserve, compre e avalie livros"],
                  ["FUNCIONARIO", "Funcionário", "Gerencie o acervo da biblioteca"],
                ].map(([valor, rotulo, descricao]) => (
                  <button
                    type="button"
                    key={valor}
                    role="radio"
                    aria-checked={formUsuario.tipo === valor}
                    className={
                      "tipo-conta-opcao" +
                      (formUsuario.tipo === valor ? " ativa" : "")
                    }
                    onClick={() =>
                      setFormUsuario({
                        ...formUsuario,
                        tipo: valor,
                        codigoAcesso: "",
                      })
                    }
                  >
                    <strong>{rotulo}</strong>
                    <span>{descricao}</span>
                  </button>
                ))}
              </div>
            </div>
            {formUsuario.tipo === "FUNCIONARIO" && (
              <CampoSenha
                rotulo="Código de acesso de funcionário"
                autoComplete="off"
                valor={formUsuario.codigoAcesso}
                aoMudar={(codigoAcesso) =>
                  setFormUsuario({ ...formUsuario, codigoAcesso })
                }
              />
            )}

            <div className="aceite-termos">
              <div className="aceite-documentos">
                <button
                  type="button"
                  className="aceite-link"
                  onClick={() => setDocumentoAberto("termos")}
                >
                  Ler Termos de Uso
                </button>
                <button
                  type="button"
                  className="aceite-link"
                  onClick={() => setDocumentoAberto("privacidade")}
                >
                  Ler Política de Privacidade
                </button>
              </div>
              <label className="aceite-checkbox">
                <input
                  type="checkbox"
                  checked={formUsuario.aceitouTermos}
                  onChange={(e) =>
                    setFormUsuario({
                      ...formUsuario,
                      aceitouTermos: e.target.checked,
                    })
                  }
                />
                <span>
                  Li e aceito os Termos de Uso e a Política de Privacidade.
                </span>
              </label>
            </div>

            {erro && <p className="erro">{erro}</p>}

            <div className="modal-actions">
              <button
                type="submit"
                disabled={salvando || !formUsuario.aceitouTermos}
              >
                {salvando ? "Salvando..." : "Criar minha conta"}
              </button>
            </div>
            <p className="auth-alternar">
              Já tem conta?{" "}
              <button
                type="button"
                className="link-button"
                onClick={() => trocarModoAutenticacao("login")}
              >
                Entrar
              </button>
            </p>
          </form>
        )}
        </div>
      </div>
      {modalDocumento}
    </div>
  );

  const avisoDaPagina = aviso && (
    <p className="aviso" onAnimationEnd={() => setAviso(null)}>
      {aviso}
    </p>
  );

  const modalDetalhesDoPedido = pedidoDetalhado && (
    <div className="modal" onClick={() => setPedidoDetalhado(null)}>
      <div
        className="modal-box pedido-detalhes"
        role="dialog"
        aria-modal="true"
        aria-labelledby="pedido-detalhes-titulo"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="modal-header">
          <div>
            <span
              className={
                "reserva-status " + pedidoDetalhado.status.toLowerCase()
              }
            >
              {ROTULO_STATUS_PEDIDO[pedidoDetalhado.status] ||
                pedidoDetalhado.status}
            </span>
            <h3 id="pedido-detalhes-titulo">
              Pedido #{numeroDoPedido(pedidoDetalhado.id)}
            </h3>
            <p className="modal-sub">
              Feito em {formatarData(pedidoDetalhado.dataCriacao)}
            </p>
          </div>
          <button
            type="button"
            onClick={() => setPedidoDetalhado(null)}
            aria-label="Fechar detalhes do pedido"
          >
            ✕
          </button>
        </div>

        <ul className="pedido-detalhes-itens">
          {(pedidoDetalhado.itens || []).map((item) => (
            <li key={item.livroId}>
              <div className="pedido-detalhes-capa">
                <CapaDoItem capaUrl={item.capaUrl} titulo={item.titulo} />
              </div>
              <div className="pedido-detalhes-livro">
                <strong>{item.titulo}</strong>
                <span>
                  {item.quantidade}{" "}
                  {item.quantidade === 1 ? "unidade" : "unidades"} ×{" "}
                  {formatarPreco(item.precoUnitario)}
                </span>
                <button
                  type="button"
                  className="link-button"
                  onClick={() => {
                    const livroDoItem = livros.find(
                      (livro) => livro.id === item.livroId
                    );
                    setPedidoDetalhado(null);
                    if (livroDoItem) {
                      abrirLivro(livroDoItem);
                    }
                  }}
                  disabled={!livros.some((livro) => livro.id === item.livroId)}
                >
                  Ver livro →
                </button>
              </div>
              <strong className="pedido-detalhes-subtotal">
                {formatarPreco(
                  Number(item.precoUnitario || 0) * Number(item.quantidade || 0)
                )}
              </strong>
            </li>
          ))}
        </ul>

        <div className="pedido-detalhes-resumo">
          {pedidoDetalhado.status === "AGUARDANDO_PAGAMENTO" && (
            <p>Pague até {formatarData(pedidoDetalhado.dataExpiracao)}.</p>
          )}
          {pedidoDetalhado.status === "APROVADO" && (
            <p>Pagamento aprovado pelo Mercado Pago.</p>
          )}
          {pedidoDetalhado.status === "EXPIRADO" && (
            <p>O prazo de pagamento terminou ou o pagamento foi cancelado.</p>
          )}
          <div>
            <span>Total</span>
            <strong>{formatarPreco(pedidoDetalhado.total)}</strong>
          </div>
        </div>

        <div className="modal-actions">
          {pedidoDetalhado.status === "AGUARDANDO_PAGAMENTO" &&
            pedidoDetalhado.linkPagamento && (
              <button
                type="button"
                onClick={() => irParaMercadoPago(pedidoDetalhado.linkPagamento)}
              >
                Pagar com Mercado Pago
              </button>
            )}
          <button
            type="button"
            className="pedido-detalhes-fechar"
            onClick={() => setPedidoDetalhado(null)}
          >
            Fechar
          </button>
        </div>
      </div>
    </div>
  );

  const termosLiberados = termosLidos.termos && termosLidos.privacidade;

  if (termosPendentes) {
    return (
      <div className="app">
        {cabecalho}
        <div className="modal">
          <div
            className="modal-box dialogo termos"
            role="alertdialog"
            aria-modal="true"
            aria-labelledby="termos-titulo"
          >
            <div className="dialogo-icone" aria-hidden="true">
              §
            </div>
            <h3 id="termos-titulo">Os termos foram atualizados</h3>
            <p>
              Atualizamos os Termos de Uso e a Política de Privacidade. Para
              continuar usando a Pallanthir, leia os dois documentos e aceite a
              nova versão.
            </p>
            <div className="termos-documentos">
              <button
                type="button"
                className={"termos-documento" + (termosLidos.termos ? " lido" : "")}
                onClick={() => setDocumentoAberto("termos")}
              >
                {termosLidos.termos ? "✓ " : ""}Termos de Uso
              </button>
              <button
                type="button"
                className={
                  "termos-documento" + (termosLidos.privacidade ? " lido" : "")
                }
                onClick={() => setDocumentoAberto("privacidade")}
              >
                {termosLidos.privacidade ? "✓ " : ""}Política de Privacidade
              </button>
            </div>
            <label
              className={"termos-aceite" + (termosLiberados ? "" : " bloqueado")}
            >
              <input
                type="checkbox"
                checked={aceiteTermos}
                disabled={!termosLiberados}
                onChange={(e) => setAceiteTermos(e.target.checked)}
              />
              Li e aceito os Termos de Uso e a Política de Privacidade
            </label>
            {!termosLiberados && (
              <p className="termos-dica">
                Abra os dois documentos para liberar o aceite.
              </p>
            )}
            {erroTermos && <p className="erro">{erroTermos}</p>}
            <div className="dialogo-acoes">
              <button
                type="button"
                className="dialogo-secundario"
                onClick={sair}
                disabled={salvando}
              >
                Sair da conta
              </button>
              <button
                type="button"
                className="dialogo-principal"
                onClick={aceitarTermosAtualizados}
                disabled={!aceiteTermos || salvando}
              >
                {salvando ? "Salvando..." : "Aceitar e continuar"}
              </button>
            </div>
          </div>
        </div>
        {modalDocumento}
      </div>
    );
  }

  if (pagina === "conta") {
    return (
      <div className="app">
        {cabecalho}

        <section className="section pagina-conta">
          <div className="section-header">
            <div>
              <span className="section-label">SUA CONTA</span>
              <h2>Minha conta</h2>
            </div>
            <button type="button" className="link-button" onClick={irParaInicio}>
              Voltar ao acervo →
            </button>
          </div>

          {avisoDaPagina}
          {erro && <p className="erro">{erro}</p>}

          {!usuario && (
            <p>
              <button
                type="button"
                className="link-button"
                onClick={() => abrirAutenticacao("login")}
              >
                Entre na sua conta
              </button>{" "}
              para ver seus dados.
            </p>
          )}

          {usuario && (
            <>
              <div className="conta-perfil">
                <div className="conta-avatar" aria-hidden="true">
                  {iniciaisDoNome(usuario.nome)}
                </div>
                <div className="conta-perfil-dados">
                  <strong>{usuario.nome}</strong>
                  <span>{usuario.email}</span>
                </div>
                <div className="conta-perfil-selos">
                  <span className="conta-selo">
                    {ROTULO_TIPO_USUARIO[usuario.tipo] || usuario.tipo}
                  </span>
                </div>
                <button type="button" className="conta-sair" onClick={sair}>
                  Sair da conta
                </button>
              </div>

              <div className="conta-grid">
                <div className="conta-bloco conta-dados">
                  <span className="section-label">DADOS PESSOAIS</span>
                  <h3>Suas informações</h3>
                  <p className="conta-bloco-sub">
                    Mantenha seu nome e e-mail atualizados.
                  </p>

                  <form className="modal-form" onSubmit={atualizarConta}>
                    <label>
                      Nome
                      <input
                        type="text"
                        required
                        value={formConta.nome}
                        onChange={(e) =>
                          setFormConta({ ...formConta, nome: e.target.value })
                        }
                      />
                    </label>
                    <label>
                      E-mail
                      <input
                        type="email"
                        required
                        value={formConta.email}
                        onChange={(e) =>
                          setFormConta({ ...formConta, email: e.target.value })
                        }
                      />
                    </label>
                    <small className="dica-senha">
                      Se você alterar o e-mail, precisará entrar novamente.
                    </small>

                    <div className="modal-actions">
                      <button type="submit" disabled={salvando}>
                        {salvando ? "Salvando..." : "Salvar alterações"}
                      </button>
                    </div>
                  </form>
                </div>

                <div className="conta-lateral">
                  <div className="conta-bloco">
                    <span className="section-label">SEGURANÇA</span>
                    <h3>Senha</h3>
                    <p className="conta-bloco-sub">
                      Para trocar sua senha, enviaremos um código de
                      confirmação para o seu e-mail.
                    </p>
                    <button
                      type="button"
                      className="conta-botao"
                      onClick={iniciarTrocaDeSenha}
                      disabled={salvando}
                    >
                      Alterar senha
                    </button>
                  </div>

                  <div className="conta-bloco">
                    <span className="section-label">DOCUMENTOS</span>
                    <h3>Termos e privacidade</h3>
                    <p className="conta-bloco-sub">
                      Consulte quando quiser os documentos que você aceitou.
                    </p>
                    {dataAceiteTermos && (
                      <p className="conta-aceite">
                        <span aria-hidden="true">✓</span>
                        Você aceitou a versão atual em{" "}
                        {formatarData(dataAceiteTermos)}
                      </p>
                    )}
                    <div className="conta-documentos">
                      <button
                        type="button"
                        onClick={() => setDocumentoAberto("termos")}
                      >
                        <span>Termos de Uso</span>
                        <span aria-hidden="true">→</span>
                      </button>
                      <button
                        type="button"
                        onClick={() => setDocumentoAberto("privacidade")}
                      >
                        <span>Política de Privacidade</span>
                        <span aria-hidden="true">→</span>
                      </button>
                    </div>
                  </div>
                </div>
              </div>

              <div className="conta-perigo">
                <div>
                  <span className="section-label">ZONA DE PERIGO</span>
                  <h3>Excluir conta</h3>
                  <p className="conta-bloco-sub">
                    Seus dados pessoais serão anonimizados e você perderá o
                    acesso aos seus favoritos e reservas.
                  </p>
                </div>
                <button
                  type="button"
                  className="perigo"
                  onClick={excluirConta}
                  disabled={salvando}
                >
                  Excluir minha conta
                </button>
              </div>
            </>
          )}
        </section>

        {modalCadastroLivro}
        {modalAutenticacao}
        {modalExclusaoConta}
        {!mostrarAutenticacao && modalDocumento}
      </div>
    );
  }

  if (pagina === "gestao") {
    return (
      <div className="app">
        {cabecalho}

        <section className="section pagina-gestao">
          <div className="section-header">
            <div>
              <span className="section-label">FUNCIONÁRIO</span>
              <h2>Gestão de reservas</h2>
            </div>
            <button type="button" className="link-button" onClick={irParaInicio}>
              Voltar ao acervo →
            </button>
          </div>

          <p className="materias-sub">
            Selecione um estudante para ver as reservas dele e registrar as
            entregas e devoluções.
          </p>

          {avisoDaPagina}
          {erro && <p className="erro">{erro}</p>}

          {!ehFuncionario && <p>Área restrita a funcionários.</p>}

          {ehFuncionario && (
            <div className="gestao">
              <aside className="gestao-usuarios">
                <input
                  type="text"
                  className="gestao-busca"
                  placeholder="Filtrar por nome ou e-mail"
                  value={filtroUsuarios}
                  onChange={(e) => setFiltroUsuarios(e.target.value)}
                />
                {usuariosFiltrados.length === 0 && (
                  <p className="best-subtitle">Nenhum estudante encontrado.</p>
                )}
                {usuariosFiltrados.map((item) => (
                  <button
                    type="button"
                    key={item.id}
                    className={
                      "gestao-usuario" +
                      (usuarioGestao && usuarioGestao.id === item.id
                        ? " ativo"
                        : "")
                    }
                    onClick={() => selecionarUsuarioGestao(item)}
                  >
                    <strong>{item.nome}</strong>
                    <small>{item.email}</small>
                  </button>
                ))}
              </aside>

              <div className="gestao-reservas">
                {!usuarioGestao && (
                  <p>Nenhum estudante selecionado.</p>
                )}
                {usuarioGestao && (
                  <h3 className="gestao-titulo">
                    Reservas de {usuarioGestao.nome}
                  </h3>
                )}
                {usuarioGestao && carregandoPainel && <p>Carregando reservas...</p>}
                {usuarioGestao &&
                  !carregandoPainel &&
                  reservasGestao.length === 0 && (
                    <p>Este estudante ainda não fez reservas.</p>
                  )}

                {usuarioGestao && reservasGestao.length > 0 && (
                  <div className="reservas">
                    {reservasGestao.map((reserva) => (
                      <div className="reserva-card" key={reserva.id}>
                        <div className="reserva-info">
                          <span
                            className={
                              "reserva-status " + reserva.status.toLowerCase()
                            }
                          >
                            {ROTULO_STATUS_RESERVA[reserva.status] ||
                              reserva.status}
                          </span>
                          <h3>{reserva.tituloLivro}</h3>
                          <small>
                            Reservado em {formatarData(reserva.dataReserva)}
                          </small>
                        </div>

                        {reserva.status === "RESERVADO" && (
                          <div className="reserva-acoes">
                            <button
                              type="button"
                              className="reservar-button"
                              onClick={() => entregarLivro(reserva)}
                              disabled={reservaEmEdicao === reserva.id}
                            >
                              Registrar entrega
                            </button>
                          </div>
                        )}
                        {reserva.status === "ENTREGUE" && (
                          <div className="reserva-acoes">
                            <button
                              type="button"
                              className="reservar-button"
                              onClick={() => devolverLivro(reserva)}
                              disabled={reservaEmEdicao === reserva.id}
                            >
                              Registrar devolução
                            </button>
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          )}
        </section>

        {modalCadastroLivro}
        {modalAutenticacao}
      </div>
    );
  }

  if (pagina === "dashboard") {
    const totalReservas = dashboard
      ? dashboard.reservas.reservadas +
        dashboard.reservas.entregues +
        dashboard.reservas.devolvidas +
        dashboard.reservas.canceladas
      : 0;

    return (
      <div className="app">
        {cabecalho}

        <section className="section pagina-dashboard">
          <div className="section-header">
            <div>
              <span className="section-label">PAINEL</span>
              <h2>Dashboard</h2>
            </div>
            <button type="button" className="link-button" onClick={irParaInicio}>
              Voltar ao acervo →
            </button>
          </div>

          <div className="dashboard-topo">
            <p className="materias-sub">
              Resumo do acervo, das reservas e das vendas da biblioteca.
            </p>
            {ehFuncionario && (
              <button
                type="button"
                className="favoritar-button"
                onClick={carregarDashboard}
                disabled={carregandoPainel}
              >
                {carregandoPainel ? "Carregando..." : "Atualizar"}
              </button>
            )}
          </div>

          {erro && <p className="erro">{erro}</p>}

          {!ehFuncionario && <p>Área restrita a funcionários.</p>}
          {ehFuncionario && !dashboard && carregandoPainel && (
            <p>Carregando dashboard...</p>
          )}

          {ehFuncionario && dashboard && (
            <>
              <h3 className="dashboard-titulo">Acervo</h3>
              <div className="dashboard-numeros">
                <div className="dashboard-numero">
                  <small>Títulos ativos</small>
                  <strong>{formatarNumero(dashboard.estoque.titulosAtivos)}</strong>
                </div>
                <div className="dashboard-numero">
                  <small>Exemplares para reserva</small>
                  <strong>
                    {formatarNumero(dashboard.estoque.exemplaresParaReserva)}
                  </strong>
                </div>
                <div className="dashboard-numero">
                  <small>Exemplares à venda</small>
                  <strong>
                    {formatarNumero(dashboard.estoque.exemplaresParaCompra)}
                  </strong>
                </div>
                <div className="dashboard-numero">
                  <small>Títulos esgotados para reserva</small>
                  <strong>
                    {formatarNumero(dashboard.estoque.esgotadosParaReserva)}
                  </strong>
                </div>
              </div>

              <h3 className="dashboard-titulo">Reservas</h3>
              <div className="dashboard-numeros">
                {[
                  ["RESERVADO", dashboard.reservas.reservadas],
                  ["ENTREGUE", dashboard.reservas.entregues],
                  ["DEVOLVIDO", dashboard.reservas.devolvidas],
                  ["CANCELADO", dashboard.reservas.canceladas],
                ].map(([status, quantidade]) => (
                  <div className="dashboard-numero" key={status}>
                    <span className={"reserva-status " + status.toLowerCase()}>
                      {ROTULO_STATUS_RESERVA[status]}
                    </span>
                    <strong>{formatarNumero(quantidade)}</strong>
                    <small>
                      {formatarPorcentagem(quantidade, totalReservas)} do total
                    </small>
                  </div>
                ))}
              </div>

              <div className="dashboard-graficos">
                <GraficoMaterias
                  titulo="Matérias mais reservadas"
                  descricao="As 5 matérias com mais reservas."
                  itens={dashboard.materiasMaisReservadas || []}
                  unidade="reservas"
                />
                <GraficoMaterias
                  titulo="Matérias mais compradas"
                  descricao="Exemplares vendidos em pedidos aprovados."
                  itens={dashboard.materiasMaisCompradas || []}
                  unidade="exemplares"
                />
              </div>
            </>
          )}
        </section>

        {modalCadastroLivro}
        {modalAutenticacao}
      </div>
    );
  }

  if (pagina === "logs") {
    return (
      <div className="app">
        {cabecalho}

        <section className="section pagina-logs">
          <div className="section-header">
            <div>
              <span className="section-label">AUDITORIA</span>
              <h2>Logs do sistema</h2>
            </div>
            <button type="button" className="link-button" onClick={irParaInicio}>
              Voltar ao acervo →
            </button>
          </div>

          <p className="materias-sub">
            Registro das ações feitas no sistema, das mais recentes para as mais
            antigas.
          </p>

          {erro && <p className="erro">{erro}</p>}

          {!ehFuncionario && <p>Área restrita a funcionários.</p>}

          {ehFuncionario && (
            <>
              <div className="logs-painel">
                <div className="logs-painel-topo">
                  <div>
                    <strong>Filtrar registros</strong>
                    <span>
                      Combine os filtros para encontrar uma ação específica.
                    </span>
                  </div>
                  <div className="logs-painel-acoes">
                    {filtrosDeLogsAtivos && (
                      <button
                        type="button"
                        className="logs-limpar"
                        onClick={() =>
                          setFiltroLogs({
                            ...FILTRO_LOGS_VAZIO,
                            usuarioId: filtroLogs.usuarioId,
                          })
                        }
                      >
                        ✕ Limpar filtros
                      </button>
                    )}
                    <button
                      type="button"
                      className="logs-atualizar"
                      onClick={() => carregarLogs(filtroLogs.usuarioId)}
                      disabled={carregandoPainel}
                    >
                      {carregandoPainel ? "Carregando..." : "↻ Atualizar"}
                    </button>
                  </div>
                </div>

                <div className="logs-filtros">
                  <label>
                    Usuário
                    <select
                      value={filtroLogs.usuarioId}
                      onChange={(e) => {
                        setFiltroLogs({ ...filtroLogs, usuarioId: e.target.value });
                        carregarLogs(e.target.value);
                      }}
                    >
                      <option value="">Todos os usuários</option>
                      {usuariosGestao.map((item) => (
                        <option key={item.id} value={item.id}>
                          {item.nome} ({item.email})
                        </option>
                      ))}
                    </select>
                  </label>
                  <label>
                    Ação
                    <select
                      aria-label="Filtrar por ação"
                      value={filtroLogs.acao}
                      onChange={(e) =>
                        setFiltroLogs({ ...filtroLogs, acao: e.target.value })
                      }
                    >
                      <option value="">Todas as ações</option>
                      {acoesDosLogs.map((acao) => (
                        <option key={acao} value={acao}>
                          {acao}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label>
                    De
                    <input
                      type="date"
                      value={filtroLogs.de}
                      max={filtroLogs.ate || undefined}
                      onChange={(e) =>
                        setFiltroLogs({ ...filtroLogs, de: e.target.value })
                      }
                    />
                  </label>
                  <label>
                    Até
                    <input
                      type="date"
                      value={filtroLogs.ate}
                      min={filtroLogs.de || undefined}
                      onChange={(e) =>
                        setFiltroLogs({ ...filtroLogs, ate: e.target.value })
                      }
                    />
                  </label>
                  <label className="logs-busca">
                    Buscar
                    <input
                      type="text"
                      placeholder="Palavra da descrição ou e-mail do usuário"
                      value={filtroLogs.texto}
                      onChange={(e) =>
                        setFiltroLogs({ ...filtroLogs, texto: e.target.value })
                      }
                    />
                  </label>
                </div>
              </div>

              {periodoDosLogsInvalido && (
                <p className="erro">
                  A data inicial precisa ser igual ou anterior à data final.
                </p>
              )}

              {!carregandoPainel && logs.length > 0 && (
                <p className="logs-contagem">
                  {(logsExibidos.length === 1
                    ? "1 registro"
                    : logsExibidos.length + " registros") +
                    (filtrosDeLogsAtivos ? " com os filtros escolhidos" : "")}
                </p>
              )}

              {!carregandoPainel && logsExibidos.length === 0 && (
                <p>Nenhum registro encontrado.</p>
              )}

              {logsExibidos.length > 0 && (
                <div className="logs-tabela-container">
                  <table className="logs-tabela">
                    <thead>
                      <tr>
                        <th>Data</th>
                        <th>Ação</th>
                        <th>Usuário</th>
                        <th>Descrição</th>
                      </tr>
                    </thead>
                    <tbody>
                      {logsExibidos.map((log) => (
                        <tr key={log.id}>
                          <td className="logs-data">{formatarData(log.dataHora)}</td>
                          <td>
                            <span className={classeDaAcao(log.acao)}>
                              {log.acao}
                            </span>
                          </td>
                          <td>{log.usuarioEmail || "—"}</td>
                          <td>{log.descricao}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </>
          )}
        </section>

        {modalCadastroLivro}
        {modalAutenticacao}
      </div>
    );
  }

  if (pagina === "materias") {
    return (
      <div className="app">
        {cabecalho}

        <section className="section pagina-materias">
          <div className="section-header">
            <div>
              <span className="section-label">EXPLORE</span>
              <h2>Matérias</h2>
            </div>
            <button type="button" className="link-button" onClick={irParaInicio}>
              Voltar ao acervo →
            </button>
          </div>

          <p className="materias-sub">
            Escolha uma matéria para ver os livros do acervo relacionados a ela.
          </p>

          {erro && <p className="erro">{erro}</p>}

          <div className="categories">
            {materias.length === 0 && <p>Carregando matérias...</p>}
            {materias.map((materia) => (
              <button
                type="button"
                key={materia.nome}
                className={
                  "category" +
                  (filtro.tipo === "materia" && filtro.nome === materia.nome
                    ? " ativa"
                    : "")
                }
                onClick={() => filtrarPorMateria(materia)}
              >
                <div className="category-icon">
                  {ICONES_MATERIA[materia.nome] || "📚"}
                </div>
                <h3>{materia.rotulo}</h3>
                <p>
                  {materia.quantidade}{" "}
                  {materia.quantidade === 1 ? "livro" : "livros"}
                </p>
              </button>
            ))}
          </div>
        </section>

        {modalCadastroLivro}
        {modalAutenticacao}
      </div>
    );
  }

  if (pagina === "ranking") {
    return (
      <div className="app">
        {cabecalho}

        <section className="section pagina-ranking">
          <div className="section-header">
            <div>
              <span className="section-label">RANKING</span>
              <h2>Top 10 mais bem avaliados</h2>
            </div>
            <button type="button" className="link-button" onClick={irParaInicio}>
              Voltar ao acervo →
            </button>
          </div>

          <p className="materias-sub">
            Os 10 livros com a maior média de notas dadas pelos estudantes.
            Entram no ranking os livros com pelo menos 3 avaliações.
          </p>

          {carregandoRanking && <p>Carregando ranking...</p>}
          {!carregandoRanking && erroRanking && (
            <p className="erro">{erroRanking}</p>
          )}
          {!carregandoRanking && !erroRanking && rankingDeLivros.length === 0 && (
            <p>Nenhum livro tem avaliações suficientes para o ranking ainda.</p>
          )}

          <ol className="ranking-lista">
            {rankingDeLivros.map((livro, indice) => (
              <li key={livro.id}>
                <button
                  type="button"
                  className={"ranking-item" + (indice < 3 ? " podio" : "")}
                  onClick={() => abrirLivro(livro)}
                >
                  <span className="ranking-posicao">
                    {String(indice + 1).padStart(2, "0")}
                  </span>
                  <div className="ranking-capa">
                    {livro.capaUrl ? (
                      <img
                        src={capaNaResolucao(livro.capaUrl, ZOOM_CAPA_CARTAO)}
                        alt={"Capa de " + livro.titulo}
                        loading="lazy"
                      />
                    ) : (
                      <CapaGenerica titulo={livro.titulo} tamanho="mini" />
                    )}
                  </div>
                  <div className="ranking-info">
                    {livro.materia && (
                      <span className="book-category">{livro.materia}</span>
                    )}
                    <strong>{livro.titulo}</strong>
                    {(livro.autores || []).length > 0 && (
                      <small>{livro.autores.join(", ")}</small>
                    )}
                    <small>{formatarTotalAvaliacoes(livro.total)}</small>
                  </div>
                  <div className="ranking-nota">
                    <span>★</span>
                    {formatarNota(livro.media)}
                  </div>
                </button>
              </li>
            ))}
          </ol>
        </section>

        {modalCadastroLivro}
        {modalAutenticacao}
      </div>
    );
  }

  if (pagina === "reservas") {
    return (
      <div className="app">
        {cabecalho}

        <section className="section pagina-reservas">
          <div className="section-header">
            <div>
              <span className="section-label">SUAS RESERVAS</span>
              <h2>Minhas reservas</h2>
            </div>
            <button type="button" className="link-button" onClick={irParaInicio}>
              Voltar ao acervo →
            </button>
          </div>

          {aviso && (
            <p className="aviso" onAnimationEnd={() => setAviso(null)}>
              {aviso}
            </p>
          )}
          {erro && <p className="erro">{erro}</p>}

          {!usuario && (
            <p>
              <button
                type="button"
                className="link-button"
                onClick={() => abrirAutenticacao("login")}
              >
                Entre na sua conta
              </button>{" "}
              para ver suas reservas.
            </p>
          )}
          {usuario && reservas.length > 0 && (
            <p className="materias-sub">
              Retire o livro reservado na biblioteca. Depois da entrega, a
              reserva não pode mais ser cancelada. Para devolver, leve-o até a
              biblioteca e um funcionário registrará a devolução.
            </p>
          )}
          {usuario && reservas.length === 0 && (
            <p>Você ainda não reservou nenhum livro.</p>
          )}

          {usuario && reservas.length > 0 && (
            <div className="reservas">
              {reservas.map((reserva) => (
                <div className="reserva-card" key={reserva.id}>
                  <div className="reserva-info">
                    <span
                      className={
                        "reserva-status " + reserva.status.toLowerCase()
                      }
                    >
                      {ROTULO_STATUS_RESERVA[reserva.status] || reserva.status}
                    </span>
                    <h3>{reserva.tituloLivro}</h3>
                    <small>Reservado em {formatarData(reserva.dataReserva)}</small>
                  </div>

                  {reserva.status === "RESERVADO" && (
                    <div className="reserva-acoes">
                      <button
                        type="button"
                        className="reservar-button cancelar"
                        onClick={() => cancelarReserva(reserva)}
                        disabled={reservaEmEdicao === reserva.id}
                      >
                        Cancelar
                      </button>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </section>

        {modalCadastroLivro}
        {modalAutenticacao}
      </div>
    );
  }

  if (pagina === "carrinho") {
    return (
      <div className="app">
        {cabecalho}

        <section className="section pagina-reservas">
          <div className="section-header">
            <div>
              <span className="section-label">SUA COMPRA</span>
              <h2>Meu carrinho</h2>
            </div>
            <div className="carrinho-links">
              <button
                type="button"
                className="link-button"
                onClick={irParaPedidos}
              >
                Meus pedidos
              </button>
              <button
                type="button"
                className="link-button"
                onClick={irParaInicio}
              >
                Voltar ao acervo →
              </button>
            </div>
          </div>

          {aviso && (
            <p className="aviso" onAnimationEnd={() => setAviso(null)}>
              {aviso}
            </p>
          )}
          {erro && <p className="erro">{erro}</p>}

          {!usuario && (
            <p>
              <button
                type="button"
                className="link-button"
                onClick={() => abrirAutenticacao("login")}
              >
                Entre na sua conta
              </button>{" "}
              para ver o seu carrinho.
            </p>
          )}
          {usuario && !ehEstudante && (
            <p>As compras estão disponíveis apenas para estudantes.</p>
          )}
          {ehEstudante && carrinho.itens.length === 0 && (
            <p>Seu carrinho está vazio.</p>
          )}

          {ehEstudante && carrinho.itens.length > 0 && pedidoAguardando && (
            <p className="retorno-pagamento pendente">
              O pedido #{numeroDoPedido(pedidoAguardando.id)} está aguardando pagamento até{" "}
              {formatarData(pedidoAguardando.dataExpiracao)}. Os itens saem do
              carrinho quando o pagamento for aprovado ou o prazo terminar.
            </p>
          )}

          {ehEstudante && carrinho.itens.length > 0 && (
            <>
              <div className="reservas">
                {carrinho.itens.map((item) => (
                  <div className="reserva-card carrinho-item" key={item.livroId}>
                    <div className="carrinho-capa">
                      {item.capaUrl ? (
                        <img
                          src={capaNaResolucao(item.capaUrl, ZOOM_CAPA_CARTAO)}
                          alt={"Capa de " + item.titulo}
                          loading="lazy"
                        />
                      ) : (
                        <CapaGenerica titulo={item.titulo} tamanho="cartao" />
                      )}
                    </div>
                    <div className="reserva-info carrinho-info">
                      <h3>{item.titulo}</h3>
                      <small>{formatarPreco(item.precoUnitario)} por unidade</small>
                    </div>
                    <div
                      className="carrinho-quantidade"
                      role="group"
                      aria-label={"Quantidade de " + item.titulo}
                    >
                      <button
                        type="button"
                        onClick={() =>
                          alterarQuantidadeNoCarrinho(item, item.quantidade - 1)
                        }
                        disabled={
                          temPedidoAguardando ||
                          itemCarrinhoEmEdicao === item.livroId ||
                          item.quantidade <= 1
                        }
                        aria-label="Diminuir quantidade"
                      >
                        −
                      </button>
                      <span>{item.quantidade}</span>
                      <button
                        type="button"
                        onClick={() =>
                          alterarQuantidadeNoCarrinho(item, item.quantidade + 1)
                        }
                        disabled={
                          temPedidoAguardando ||
                          itemCarrinhoEmEdicao === item.livroId
                        }
                        aria-label="Aumentar quantidade"
                      >
                        +
                      </button>
                    </div>
                    <strong className="carrinho-subtotal">
                      {formatarPreco(item.subtotal)}
                    </strong>
                    <div className="reserva-acoes">
                      <button
                        type="button"
                        className="reservar-button cancelar"
                        onClick={() => removerDoCarrinho(item)}
                        disabled={
                          temPedidoAguardando ||
                          itemCarrinhoEmEdicao === item.livroId
                        }
                      >
                        Remover
                      </button>
                    </div>
                  </div>
                ))}
              </div>

              <div className="carrinho-resumo">
                <div>
                  <small>Total</small>
                  <strong>{formatarPreco(carrinho.total)}</strong>
                </div>
                <p className="materias-sub">
                  {pedidoAguardando
                    ? "Continue o pagamento do pedido em andamento no Mercado Pago."
                    : "Você será levado ao Mercado Pago para concluir o pagamento. Os itens ficam separados por 30 minutos."}
                </p>
                <button
                  type="button"
                  className="reservar-button"
                  onClick={finalizarCompra}
                  disabled={finalizandoCompra}
                >
                  {finalizandoCompra
                    ? "Redirecionando..."
                    : "Pagar com Mercado Pago"}
                </button>
              </div>
            </>
          )}
        </section>

        {modalCadastroLivro}
        {modalAutenticacao}
      </div>
    );
  }

  if (pagina === "pedidos") {
    return (
      <div className="app">
        {cabecalho}

        <section className="section pagina-reservas">
          <div className="section-header">
            <div>
              <span className="section-label">SUAS COMPRAS</span>
              <h2>Meus pedidos</h2>
            </div>
            <button type="button" className="link-button" onClick={irParaInicio}>
              Voltar ao acervo →
            </button>
          </div>

          {retornoPagamento && (
            <p className={"retorno-pagamento " + retornoPagamento.tipo}>
              {retornoPagamento.mensagem}
            </p>
          )}
          {aviso && (
            <p className="aviso" onAnimationEnd={() => setAviso(null)}>
              {aviso}
            </p>
          )}
          {erro && <p className="erro">{erro}</p>}
          {ehEstudante && erroPedidos && <p className="erro">{erroPedidos}</p>}

          {!usuario && (
            <p>
              <button
                type="button"
                className="link-button"
                onClick={() => abrirAutenticacao("login")}
              >
                Entre na sua conta
              </button>{" "}
              {retornoPagamento
                ? "para ver o status da compra. O fim da sessão não afeta o seu pagamento."
                : "para ver os seus pedidos."}
            </p>
          )}
          {usuario && !ehEstudante && (
            <p>As compras estão disponíveis apenas para estudantes.</p>
          )}
          {ehEstudante && pedidos.length === 0 && carregandoPedidos && (
            <p>Carregando pedidos...</p>
          )}
          {ehEstudante &&
            pedidos.length === 0 &&
            !carregandoPedidos &&
            !erroPedidos && <p>Você ainda não fez nenhum pedido.</p>}

          {ehEstudante && pedidos.length > 0 && (
            <div className="reservas">
              {pedidos.map((pedido) => (
                <div className="reserva-card" key={pedido.id}>
                  <div className="reserva-info">
                    <span
                      className={
                        "reserva-status " + pedido.status.toLowerCase()
                      }
                    >
                      {ROTULO_STATUS_PEDIDO[pedido.status] || pedido.status}
                    </span>
                    <h3>Pedido #{numeroDoPedido(pedido.id)}</h3>
                    <small>Feito em {formatarData(pedido.dataCriacao)}</small>
                    <ul className="pedido-itens">
                      {(pedido.itens || []).map((item) => (
                        <li key={item.livroId}>
                          {item.quantidade}x {item.titulo} ·{" "}
                          {formatarPreco(item.precoUnitario)}
                        </li>
                      ))}
                    </ul>
                    {pedido.status === "AGUARDANDO_PAGAMENTO" && (
                      <small>
                        Pague até {formatarData(pedido.dataExpiracao)}
                      </small>
                    )}
                    {pedido.status === "EXPIRADO" && (
                      <small>
                        O prazo de pagamento terminou ou o pagamento foi
                        cancelado.
                      </small>
                    )}
                    <button
                      type="button"
                      className="link-button pedido-detalhes-link"
                      onClick={() => setPedidoDetalhado(pedido)}
                    >
                      Ver detalhes →
                    </button>
                  </div>

                  <div className="pedido-lateral">
                    <strong className="carrinho-subtotal">
                      {formatarPreco(pedido.total)}
                    </strong>
                    {pedido.status === "AGUARDANDO_PAGAMENTO" &&
                      pedido.linkPagamento && (
                        <button
                          type="button"
                          className="reservar-button"
                          onClick={() =>
                            irParaMercadoPago(pedido.linkPagamento)
                          }
                        >
                          Pagar com Mercado Pago
                        </button>
                      )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>

        {modalCadastroLivro}
        {modalAutenticacao}
        {modalDetalhesDoPedido}
      </div>
    );
  }

  if (pagina === "favoritos") {
    return (
      <div className="app">
        {cabecalho}

        <section className="section pagina-favoritos">
          <div className="section-header">
            <div>
              <span className="section-label">SUA LISTA</span>
              <h2>Meus favoritos</h2>
            </div>
            <button type="button" className="link-button" onClick={irParaInicio}>
              Voltar ao acervo →
            </button>
          </div>

          {aviso && (
            <p className="aviso" onAnimationEnd={() => setAviso(null)}>
              {aviso}
            </p>
          )}
          {erro && <p className="erro">{erro}</p>}

          <div className="books">
            {!usuario && (
              <p>
                <button
                  type="button"
                  className="link-button"
                  onClick={() => abrirAutenticacao("login")}
                >
                  Entre na sua conta
                </button>{" "}
                para ver os livros que você favoritou.
              </p>
            )}
            {usuario && favoritos.length === 0 && (
              <p>Você ainda não favoritou nenhum livro.</p>
            )}
            {usuario && favoritos.map(cartaoLivro)}
          </div>
        </section>

        {modalCadastroLivro}
        {modalAutenticacao}
      </div>
    );
  }

  if (pagina === "livro" && livroAberto && formEdicao) {
    const favoritado = idsFavoritos.has(livroAberto.id);
    const reservaAberta = reservaAtivaDoLivro(livroAberto);

    return (
      <div className="app">
        {cabecalho}

        <section className="pagina-livro">
          <button type="button" className="voltar" onClick={voltarParaInicio}>
            ← Voltar para o acervo
          </button>

          {aviso && (
            <p className="aviso" onAnimationEnd={() => setAviso(null)}>
              {aviso}
            </p>
          )}
          {erro && <p className="erro">{erro}</p>}

          <div className="livro-topo">
            <div className="livro-capa">
              {livroAberto.capaUrl ? (
                <button
                  type="button"
                  className="capa-ampliar"
                  onClick={() =>
                    setCapaAmpliada({
                      capaUrl: livroAberto.capaUrl,
                      titulo: livroAberto.titulo,
                    })
                  }
                  aria-label={"Ampliar a capa de " + livroAberto.titulo}
                >
                  <img
                    src={capaNaResolucao(livroAberto.capaUrl, ZOOM_CAPA_PAGINA)}
                    alt={"Capa de " + livroAberto.titulo}
                  />
                  <span className="capa-lupa">🔍 Ampliar</span>
                </button>
              ) : (
                <CapaGenerica titulo={livroAberto.titulo} tamanho="grande" />
              )}
              {!ehFuncionario && (
                <button
                  type="button"
                  className={
                    "favorite favorite-livro" + (favoritado ? " ativo" : "")
                  }
                  onClick={() => alternarFavorito(livroAberto)}
                  disabled={favoritoEmEdicao === livroAberto.id}
                  aria-label={
                    favoritado ? "Remover dos favoritos" : "Adicionar aos favoritos"
                  }
                  title={
                    favoritado ? "Remover dos favoritos" : "Adicionar aos favoritos"
                  }
                >
                  {favoritado ? "♥" : "♡"}
                </button>
              )}
            </div>

            <form
              className={"livro-dados" + (editandoLivro ? " editando" : "")}
              onSubmit={atualizarLivro}
            >
              {editandoLivro ? (
                <input
                  className="livro-titulo-input"
                  aria-label="Título"
                  required
                  value={formEdicao.titulo || ""}
                  onChange={(e) => alterarEdicao("titulo", e.target.value)}
                />
              ) : (
                <h1>{livroAberto.titulo}</h1>
              )}
              <p className="livro-autores">
                {(livroAberto.autores || []).join(", ") || "Autor desconhecido"}
              </p>

              {editandoLivro && (
                <p className="edicao-aviso">
                  <span className="edicao-aviso-icone" aria-hidden="true">
                    ✎
                  </span>
                  <span>
                    Modo de edição: altere os campos destacados e clique em{" "}
                    <strong>Salvar alterações</strong>.
                  </span>
                </p>
              )}

              <div className="livro-atributos">
                <div className={editandoLivro ? "editavel" : undefined}>
                  <small>Ano de lançamento</small>
                  {editandoLivro ? (
                    <input
                      aria-label="Ano de lançamento"
                      value={formEdicao.anoLancamento || ""}
                      onChange={(e) =>
                        alterarEdicao("anoLancamento", e.target.value)
                      }
                    />
                  ) : (
                    <strong>{livroAberto.anoLancamento || "—"}</strong>
                  )}
                </div>
                <div className={editandoLivro ? "editavel" : undefined}>
                  <small>Páginas</small>
                  {editandoLivro ? (
                    <input
                      type="number"
                      min="0"
                      step="1"
                      aria-label="Páginas"
                      value={formEdicao.numeroPagina ?? ""}
                      onChange={(e) =>
                        alterarEdicao("numeroPagina", e.target.value)
                      }
                    />
                  ) : (
                    <strong>{livroAberto.numeroPagina || 0}</strong>
                  )}
                </div>
                <div className={editandoLivro ? "editavel" : undefined}>
                  <small>Matéria</small>
                  {editandoLivro ? (
                    <select
                      aria-label="Matéria"
                      value={nomeDaMateria(formEdicao.materia)}
                      onChange={(e) => alterarEdicao("materia", e.target.value)}
                    >
                      <option value="">Sem matéria</option>
                      {materias.map((materia) => (
                        <option key={materia.nome} value={materia.nome}>
                          {materia.rotulo}
                        </option>
                      ))}
                    </select>
                  ) : (
                    <strong>{livroAberto.materia || "Sem matéria"}</strong>
                  )}
                </div>
                <div className={editandoLivro ? "editavel" : undefined}>
                  <small>Preço (R$)</small>
                  {editandoLivro ? (
                    <input
                      inputMode="decimal"
                      aria-label="Preço"
                      value={formEdicao.preco ?? ""}
                      onChange={(e) => alterarEdicao("preco", e.target.value)}
                    />
                  ) : (
                    <strong>{formatarPreco(livroAberto.preco)}</strong>
                  )}
                </div>
                <div className={editandoLivro ? "somente-leitura" : undefined}>
                  <small>Avaliação</small>
                  <strong>
                    {resumoAvaliacoes.total > 0
                      ? "★ " + formatarNota(resumoAvaliacoes.media)
                      : "Sem avaliações"}
                  </strong>
                </div>
                <div className={editandoLivro ? "editavel" : undefined}>
                  <small>Para reserva</small>
                  {editandoLivro ? (
                    <input
                      type="number"
                      min="0"
                      step="1"
                      aria-label="Estoque para reserva"
                      value={formEdicao.estoqueReserva ?? 0}
                      onChange={(e) =>
                        alterarEdicao("estoqueReserva", e.target.value)
                      }
                    />
                  ) : (
                    <strong>{formatarEstoque(livroAberto.estoqueReserva)}</strong>
                  )}
                </div>
                <div className={editandoLivro ? "editavel" : undefined}>
                  <small>Para compra</small>
                  {editandoLivro ? (
                    <input
                      type="number"
                      min="0"
                      step="1"
                      aria-label="Estoque para compra"
                      value={formEdicao.estoqueCompra ?? 0}
                      onChange={(e) =>
                        alterarEdicao("estoqueCompra", e.target.value)
                      }
                    />
                  ) : (
                    <strong>{formatarEstoque(livroAberto.estoqueCompra)}</strong>
                  )}
                </div>
                <div className={editandoLivro ? "editavel" : undefined}>
                  <small>ISBN-10</small>
                  {editandoLivro ? (
                    <input
                      maxLength={13}
                      placeholder="Ex.: 8595084742"
                      aria-label="ISBN-10"
                      value={formEdicao.isbn10 || ""}
                      onChange={(e) => alterarEdicao("isbn10", e.target.value)}
                    />
                  ) : (
                    <strong>{livroAberto.isbn10 || "—"}</strong>
                  )}
                </div>
                <div className={editandoLivro ? "editavel" : undefined}>
                  <small>ISBN-13</small>
                  {editandoLivro ? (
                    <input
                      maxLength={17}
                      placeholder="Ex.: 978-8595084742"
                      aria-label="ISBN-13"
                      value={formEdicao.isbn13 || ""}
                      onChange={(e) => alterarEdicao("isbn13", e.target.value)}
                    />
                  ) : (
                    <strong>{formatarIsbn13(livroAberto.isbn13)}</strong>
                  )}
                </div>
              </div>

              <div className="livro-acoes">
                {ehFuncionario ? null : reservaAberta &&
                  reservaAberta.status === "ENTREGUE" ? (
                  <button type="button" className="reservar-button" disabled>
                    Livro entregue
                  </button>
                ) : reservaAberta ? (
                  <button
                    type="button"
                    className="reservar-button cancelar"
                    onClick={() => cancelarReserva(reservaAberta)}
                    disabled={reservaEmEdicao === reservaAberta.id}
                  >
                    Cancelar reserva
                  </button>
                ) : (
                  <button
                    type="button"
                    className="reservar-button"
                    onClick={() => reservarLivro(livroAberto)}
                    disabled={
                      reservaEmEdicao === livroAberto.id ||
                      Number(livroAberto.estoqueReserva || 0) <= 0
                    }
                  >
                    {Number(livroAberto.estoqueReserva || 0) <= 0
                      ? "Sem estoque"
                      : "Reservar livro"}
                  </button>
                )}
                {!ehFuncionario && (
                  <button
                    type="button"
                    className="reservar-button"
                    onClick={() => adicionarAoCarrinho(livroAberto)}
                    disabled={
                      itemCarrinhoEmEdicao === livroAberto.id ||
                      !podeSerComprado(livroAberto)
                    }
                  >
                    {podeSerComprado(livroAberto)
                      ? "Adicionar ao carrinho"
                      : "Indisponível para compra"}
                  </button>
                )}
                {ehFuncionario && !editandoLivro && (
                  <>
                    <button
                      type="button"
                      className="cadastro-button"
                      onClick={() => iniciarEdicaoDoLivro(livroAberto)}
                    >
                      ✎ Atualizar livro
                    </button>
                    <button
                      type="button"
                      className="perigo"
                      onClick={() => deletarLivro(livroAberto)}
                      disabled={salvando}
                    >
                      Excluir livro
                    </button>
                  </>
                )}
                {ehFuncionario && editandoLivro && (
                  <>
                    <button
                      type="submit"
                      className="cadastro-button"
                      disabled={salvando}
                    >
                      {salvando ? "Salvando..." : "Salvar alterações"}
                    </button>
                    <button
                      type="button"
                      className="botao-cancelar"
                      onClick={() => cancelarEdicaoDoLivro(livroAberto)}
                      disabled={salvando}
                    >
                      Cancelar
                    </button>
                  </>
                )}
              </div>
            </form>
          </div>

          <div className="avaliacoes">
            <div className="avaliacoes-cabecalho">
              <div>
                <span className="section-label">AVALIAÇÕES</span>
                <h2>O que os estudantes acham</h2>
              </div>
              <div className="avaliacoes-media">
                <strong>{formatarNota(resumoAvaliacoes.media)}</strong>
                <div>
                  <span className="estrelas">
                    {estrelasDaNota(resumoAvaliacoes.media)}
                  </span>
                  <small>{formatarTotalAvaliacoes(resumoAvaliacoes.total)}</small>
                </div>
              </div>
            </div>

            {!usuario && (
              <p className="avaliacoes-dica">
                <button
                  type="button"
                  className="link-button"
                  onClick={() => abrirAutenticacao("login")}
                >
                  Entre na sua conta
                </button>{" "}
                para avaliar este livro.
              </p>
            )}

            {ehEstudante && (
              <form className="avaliacao-form" onSubmit={enviarAvaliacao}>
                <span className="avaliacao-form-titulo">
                  {minhaAvaliacao ? "Sua avaliação" : "Avalie este livro"}
                </span>
                <div
                  className="seletor-estrelas"
                  role="radiogroup"
                  aria-label="Nota"
                >
                  {NOTAS.map((nota) => (
                    <button
                      type="button"
                      key={nota}
                      role="radio"
                      aria-checked={formAvaliacao.nota === nota}
                      aria-label={nota + (nota === 1 ? " estrela" : " estrelas")}
                      className={nota <= formAvaliacao.nota ? "marcada" : ""}
                      onClick={() =>
                        setFormAvaliacao({ ...formAvaliacao, nota })
                      }
                    >
                      ★
                    </button>
                  ))}
                </div>
                <textarea
                  placeholder="Conte o que achou do livro (opcional)"
                  maxLength={LIMITE_COMENTARIO}
                  value={formAvaliacao.comentario}
                  onChange={(e) =>
                    setFormAvaliacao({
                      ...formAvaliacao,
                      comentario: e.target.value,
                    })
                  }
                />
                <div className="avaliacao-form-rodape">
                  <small>
                    {formAvaliacao.comentario.length}/{LIMITE_COMENTARIO}
                  </small>
                  <div className="avaliacao-form-acoes">
                    {minhaAvaliacao && (
                      <button
                        type="button"
                        className="perigo"
                        onClick={removerAvaliacao}
                        disabled={salvandoAvaliacao}
                      >
                        Remover
                      </button>
                    )}
                    <button type="submit" disabled={salvandoAvaliacao}>
                      {salvandoAvaliacao
                        ? "Enviando..."
                        : minhaAvaliacao
                        ? "Atualizar avaliação"
                        : "Enviar avaliação"}
                    </button>
                  </div>
                </div>
              </form>
            )}

            {carregandoAvaliacoes && <p>Carregando avaliações...</p>}
            {!carregandoAvaliacoes && resumoAvaliacoes.total === 0 && (
              <p className="avaliacoes-dica">
                Este livro ainda não foi avaliado.
              </p>
            )}

            <ul className="avaliacoes-lista">
              {(resumoAvaliacoes.avaliacoes || []).map((avaliacao) => (
                <li
                  key={avaliacao.id}
                  className={
                    "avaliacao-item" +
                    (minhaAvaliacao && minhaAvaliacao.id === avaliacao.id
                      ? " minha"
                      : "")
                  }
                >
                  <div className="avaliacao-item-topo">
                    <strong>{avaliacao.nomeUsuario}</strong>
                    <span className="estrelas">
                      {estrelasDaNota(avaliacao.nota)}
                    </span>
                  </div>
                  {avaliacao.comentario && <p>{avaliacao.comentario}</p>}
                  <small>{formatarData(avaliacao.dataAvaliacao)}</small>
                </li>
              ))}
            </ul>
          </div>

        </section>

        {capaAmpliada && (
          <VisualizadorDeCapa
            capaUrl={capaAmpliada.capaUrl}
            titulo={capaAmpliada.titulo}
            aoFechar={() => setCapaAmpliada(null)}
          />
        )}

        {modalCadastroLivro}
        {modalAutenticacao}
      </div>
    );
  }

  return (
    <div className="app">
      {cabecalho}

      <section className="hero">
        <div className="hero-content">
          <div className="hero-tag">
            <span></span>
            UM OLHAR SOBRE O CONHECIMENTO
          </div>
          <h1>
            Encontre seu próximo
            <strong> livro.</strong>
          </h1>
          <p>
            Explore livros, descubra novos conhecimentos e encontre conteúdos
            para transformar seus estudos.
          </p>

          <form className="search" onSubmit={pesquisar}>
            <input
              type="text"
              placeholder="Pesquise pelo título do livro..."
              value={busca}
              onChange={(e) => setBusca(e.target.value)}
            />
            <button type="submit">Pesquisar</button>
          </form>
        </div>
      </section>

      <section className="section" id="livros">
        <div className="section-header">
          <div>
            <span className="section-label">{rotuloDaLista}</span>
            <h2>{tituloDaLista}</h2>
          </div>
          <div className="section-actions">
            {ehFuncionario && (
              <button
                type="button"
                className="cadastro-button"
                onClick={abrirCadastroLivro}
              >
                + Cadastrar livro
              </button>
            )}
            {filtro.tipo !== "todos" && (
              <button
                type="button"
                className="link-button"
                onClick={limparFiltro}
              >
                Ver todos →
              </button>
            )}
          </div>
        </div>

        {aviso && (
          <p className="aviso" onAnimationEnd={() => setAviso(null)}>
            {aviso}
          </p>
        )}

        <div className="books">
          {carregando && <p>Carregando livros...</p>}
          {!carregando && erro && !mostrarAutenticacao && (
            <p className="erro">{erro}</p>
          )}
          {!carregando && !erro && livrosExibidos.length === 0 && (
            <p>{mensagemListaVazia}</p>
          )}

          {!carregando && livrosDaPagina.map(cartaoLivro)}
        </div>

        {!carregando && totalPaginasLivros > 1 && (
          <nav className="paginacao" aria-label="Páginas de livros">
            <button
              type="button"
              className="paginacao-seta"
              onClick={() => irParaPaginaDeLivros(paginaLivrosAtual - 1)}
              disabled={paginaLivrosAtual === 1}
            >
              ← Anterior
            </button>
            <div className="paginacao-numeros">
              {paginasVisiveis(paginaLivrosAtual, totalPaginasLivros).map(
                (numero, indice) =>
                  numero === "…" ? (
                    <span
                      key={"reticencias-" + indice}
                      className="paginacao-reticencias"
                    >
                      …
                    </span>
                  ) : (
                    <button
                      type="button"
                      key={numero}
                      className={
                        "paginacao-numero" +
                        (numero === paginaLivrosAtual ? " atual" : "")
                      }
                      aria-current={
                        numero === paginaLivrosAtual ? "page" : undefined
                      }
                      aria-label={"Página " + numero}
                      onClick={() => irParaPaginaDeLivros(numero)}
                    >
                      {numero}
                    </button>
                  )
              )}
            </div>
            <button
              type="button"
              className="paginacao-seta"
              onClick={() => irParaPaginaDeLivros(paginaLivrosAtual + 1)}
              disabled={paginaLivrosAtual === totalPaginasLivros}
            >
              Próxima →
            </button>
            <p className="paginacao-resumo">
              {"Mostrando " +
                ((paginaLivrosAtual - 1) * LIVROS_POR_PAGINA + 1) +
                "–" +
                Math.min(
                  paginaLivrosAtual * LIVROS_POR_PAGINA,
                  livrosExibidos.length
                ) +
                " de " +
                livrosExibidos.length +
                " livros"}
            </p>
          </nav>
        )}
      </section>

      {modalCadastroLivro}
      {modalAutenticacao}
    </div>
  );
}

export default App;
