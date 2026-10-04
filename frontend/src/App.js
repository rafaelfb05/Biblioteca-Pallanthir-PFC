import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import "./App.css";
import ModalDocumento from "./Componentes/ModalDocumento";
import {
  definirAoExpirarSessao,
  definirToken,
  lerDadosDoToken,
  livrosApi,
  logsApi,
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

const FORM_LIVRO_VAZIO = { titulo: "", materia: "", preco: "", estoque: "1" };
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
const FILTRO_LOGS_VAZIO = { usuarioId: "", texto: "" };
const CHAVE_SESSAO = "pallanthir:usuario";
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

const PAGINAS_PRIVADAS = ["favoritos", "reservas", "conta", "gestao", "logs"];

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
          {visivel ? "Ocultar" : "Mostrar"}
        </button>
      </div>
    </label>
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

  const [pagina, setPagina] = useState("inicio");
  const [busca, setBusca] = useState("");
  const [filtro, setFiltro] = useState({ tipo: "todos" });
  const [resultado, setResultado] = useState([]);

  const [usuario, setUsuario] = useState(null);
  const [favoritos, setFavoritos] = useState([]);
  const [favoritoEmEdicao, setFavoritoEmEdicao] = useState(null);
  const [reservas, setReservas] = useState([]);
  const [reservaEmEdicao, setReservaEmEdicao] = useState(null);

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
  const [documentoAberto, setDocumentoAberto] = useState(null);

  const [formConta, setFormConta] = useState(FORM_CONTA_VAZIO);
  const [usuariosGestao, setUsuariosGestao] = useState([]);
  const [filtroUsuarios, setFiltroUsuarios] = useState("");
  const [usuarioGestao, setUsuarioGestao] = useState(null);
  const [reservasGestao, setReservasGestao] = useState([]);
  const [logs, setLogs] = useState([]);
  const [filtroLogs, setFiltroLogs] = useState(FILTRO_LOGS_VAZIO);
  const [carregandoPainel, setCarregandoPainel] = useState(false);

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

  const carregarDadosDoUsuario = useCallback(
    async (sessao) => {
      if (sessao.tipo !== "ESTUDANTE") {
        return;
      }
      await Promise.all([
        carregarFavoritos(sessao.id),
        carregarReservas(sessao.id),
      ]);
    },
    [carregarFavoritos, carregarReservas]
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
    setUsuariosGestao([]);
    setUsuarioGestao(null);
    setReservasGestao([]);
    setLogs([]);
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

  const excluirConta = async () => {
    if (reservasAtivas.length > 0) {
      setErro(
        "Não é possível excluir a conta enquanto houver livros reservados ou não devolvidos."
      );
      return;
    }

    if (
      !window.confirm(
        "Excluir sua conta? Seus dados pessoais serão anonimizados e esta ação não pode ser desfeita."
      )
    ) {
      return;
    }

    try {
      setSalvando(true);
      setErro(null);
      await usuariosApi.deletar(usuario.id);
      encerrarSessao("Sua conta foi excluída.");
    } catch (e) {
      setErro(mensagemDeErro(e, "Não foi possível excluir a conta."));
      console.error(e);
    } finally {
      setSalvando(false);
    }
  };

  const irParaRanking = () => {
    abrirPagina("ranking");
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
    if (Number(livro.estoque || 0) <= 0) {
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
        Number(formLivro.estoque) || 0
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
        estoque: Number(formEdicao.estoque) || 0,
        avaliacao: Number(formEdicao.avalliacao) || 0,
      });
      setAviso('"' + atualizado.titulo + '" foi atualizado.');
      setFormEdicao({ ...atualizado });
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
      [...livros].sort((a, b) => (b.avalliacao || 0) - (a.avalliacao || 0)),
    [livros]
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

  const logsExibidos = useMemo(() => {
    const termo = filtroLogs.texto.trim().toLowerCase();
    if (!termo) {
      return logs;
    }
    return logs.filter((log) =>
      [log.acao, log.descricao, log.usuarioEmail]
        .map((valor) => String(valor || "").toLowerCase())
        .some((valor) => valor.includes(termo))
    );
  }, [logs, filtroLogs.texto]);

  const reservasAtivas = useMemo(
    () =>
      reservas.filter((reserva) =>
        ["RESERVADO", "ENTREGUE"].includes(reserva.status)
      ),
    [reservas]
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
          <span>📘</span>
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
        <div className="rating">
          ★★★★★
          <small> {Number(livro.avalliacao || 0).toFixed(1)}</small>
        </div>
        <div className="book-footer">
          <strong>{formatarPreco(livro.preco)}</strong>
          <span
            className={
              "estoque" + (Number(livro.estoque || 0) <= 0 ? " esgotado" : "")
            }
          >
            {formatarEstoque(livro.estoque)}
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
              reservaEmEdicao === livro.id || Number(livro.estoque || 0) <= 0
            }
          >
            {Number(livro.estoque || 0) <= 0 ? "Indisponível" : "Reservar"}
          </button>
        )}
      </div>
    </div>
  );

  const cabecalho = (
    <header className="header">
      <button type="button" className="logo" onClick={irParaInicio}>
        Pallanthir
      </button>
      <nav className="nav">
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
          className="nav-link"
          onClick={() => {
            voltarParaInicio();
            document
              .getElementById("livros")
              ?.scrollIntoView({ behavior: "smooth" });
          }}
        >
          Livros
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
          Ranking ♛
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
      <div className="header-actions">
        {ehFuncionario && (
          <button
            type="button"
            className="cadastro-button"
            onClick={abrirCadastroLivro}
          >
            + Cadastrar livro
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
            <button type="button" className="login-button" onClick={sair}>
              Sair
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
            Estoque
            <input
              type="number"
              min="0"
              step="1"
              placeholder="Quantidade em estoque"
              value={formLivro.estoque}
              onChange={(e) =>
                setFormLivro({ ...formLivro, estoque: e.target.value })
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

  const modalDocumento = documentoAberto && (
    <ModalDocumento
      documento={documentoAberto}
      aoFechar={() => setDocumentoAberto(null)}
      aoTrocar={setDocumentoAberto}
    />
  );

  const modalAutenticacao = mostrarAutenticacao && (
    <div className="modal">
      <div className="modal-box">
        <div className="modal-header">
          <h3>{TITULOS_AUTENTICACAO[modoAutenticacao]}</h3>
          <button type="button" onClick={fecharAutenticacao}>
            ✕
          </button>
        </div>

        {(modoAutenticacao === "login" || modoAutenticacao === "cadastro") && (
          <div className="abas-autenticacao">
            <button
              type="button"
              className={
                "aba-autenticacao" +
                (modoAutenticacao === "login" ? " ativa" : "")
              }
              onClick={() => trocarModoAutenticacao("login")}
            >
              Entrar
            </button>
            <button
              type="button"
              className={
                "aba-autenticacao" +
                (modoAutenticacao === "cadastro" ? " ativa" : "")
              }
              onClick={() => trocarModoAutenticacao("cadastro")}
            >
              Criar conta
            </button>
          </div>
        )}

        <p className="modal-sub">{subtituloAutenticacao}</p>

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

            {erro && <p className="erro">{erro}</p>}

            <div className="modal-actions">
              <button type="submit" disabled={salvando}>
                {salvando ? "Enviando código..." : "Entrar"}
              </button>
            </div>
            <div className="links-autenticacao">
              <button
                type="button"
                className="link-button"
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
            </div>
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
            <CampoSenha
              rotulo="Senha"
              autoComplete="new-password"
              valor={formUsuario.senha}
              aoMudar={(senha) => setFormUsuario({ ...formUsuario, senha })}
            />
            <small className="dica-senha">{DICA_SENHA}</small>
            <CampoSenha
              rotulo="Confirmar senha"
              autoComplete="new-password"
              valor={formUsuario.confirmarSenha}
              aoMudar={(confirmarSenha) =>
                setFormUsuario({ ...formUsuario, confirmarSenha })
              }
            />
            <label>
              Tipo de conta
              <select
                value={formUsuario.tipo}
                onChange={(e) =>
                  setFormUsuario({
                    ...formUsuario,
                    tipo: e.target.value,
                    codigoAcesso: "",
                  })
                }
              >
                <option value="ESTUDANTE">Estudante</option>
                <option value="FUNCIONARIO">Funcionário</option>
              </select>
            </label>
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
                  className="favoritar-button"
                  onClick={() => setDocumentoAberto("termos")}
                >
                  Ler Termos de Uso
                </button>
                <button
                  type="button"
                  className="favoritar-button"
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
                {salvando ? "Salvando..." : "Cadastrar"}
              </button>
            </div>
          </form>
        )}
      </div>
      {modalDocumento}
    </div>
  );

  const avisoDaPagina = aviso && (
    <p className="aviso" onAnimationEnd={() => setAviso(null)}>
      {aviso}
    </p>
  );

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

          {!usuario && <p>Entre na sua conta para ver seus dados.</p>}

          {usuario && (
            <div className="conta-grid">
              <div className="livro-edicao">
                <span className="section-label">DADOS PESSOAIS</span>
                <h2>Atualizar informações</h2>

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
                <div className="conta-card">
                  <small>Tipo de conta</small>
                  <strong>{ROTULO_TIPO_USUARIO[usuario.tipo] || usuario.tipo}</strong>
                </div>

                <div className="conta-card">
                  <small>Senha</small>
                  <p>
                    Para trocar sua senha, enviaremos um código de confirmação
                    para o seu e-mail.
                  </p>
                  <button
                    type="button"
                    className="favoritar-button"
                    onClick={iniciarTrocaDeSenha}
                    disabled={salvando}
                  >
                    Alterar senha
                  </button>
                </div>

                <div className="conta-card">
                  <small>Documentos</small>
                  <p>
                    Consulte quando quiser os termos e a política que você
                    aceitou ao criar a conta.
                  </p>
                  <div className="aceite-documentos">
                    <button
                      type="button"
                      className="favoritar-button"
                      onClick={() => setDocumentoAberto("termos")}
                    >
                      Termos de Uso
                    </button>
                    <button
                      type="button"
                      className="favoritar-button"
                      onClick={() => setDocumentoAberto("privacidade")}
                    >
                      Política de Privacidade
                    </button>
                  </div>
                </div>

                <div className="conta-card perigo-card">
                  <small>Excluir conta</small>
                  <p>
                    Seus dados pessoais serão anonimizados e você perderá o
                    acesso aos seus favoritos e reservas.
                  </p>
                  {reservasAtivas.length > 0 && (
                    <p className="erro">
                      Você possui livros reservados ou não devolvidos. Cancele
                      as reservas ou devolva os livros na biblioteca antes de
                      excluir a conta.
                    </p>
                  )}
                  <button
                    type="button"
                    className="perigo"
                    onClick={excluirConta}
                    disabled={salvando || reservasAtivas.length > 0}
                  >
                    Excluir minha conta
                  </button>
                </div>
              </div>
            </div>
          )}
        </section>

        {modalCadastroLivro}
        {modalAutenticacao}
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
              <div className="logs-filtros">
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
                <input
                  type="text"
                  placeholder="Filtrar por ação, descrição ou e-mail"
                  value={filtroLogs.texto}
                  onChange={(e) =>
                    setFiltroLogs({ ...filtroLogs, texto: e.target.value })
                  }
                />
                <button
                  type="button"
                  className="favoritar-button"
                  onClick={() => carregarLogs(filtroLogs.usuarioId)}
                  disabled={carregandoPainel}
                >
                  {carregandoPainel ? "Carregando..." : "Atualizar"}
                </button>
              </div>

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
              <h2>Mais bem avaliados</h2>
            </div>
            <button type="button" className="link-button" onClick={irParaInicio}>
              Voltar ao acervo →
            </button>
          </div>

          <p className="materias-sub">
            Os livros do acervo ordenados pela nota de avaliação.
          </p>

          {carregando && <p>Carregando livros...</p>}
          {!carregando && erro && <p className="erro">{erro}</p>}
          {!carregando && !erro && rankingDeLivros.length === 0 && (
            <p>Nenhum livro no acervo ainda.</p>
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
                      <span>📘</span>
                    )}
                  </div>
                  <div className="ranking-info">
                    <span className="book-category">{livro.materia}</span>
                    <strong>{livro.titulo}</strong>
                    <small>{(livro.autores || []).join(", ")}</small>
                  </div>
                  <div className="ranking-nota">
                    <span>★</span>
                    {Number(livro.avalliacao || 0).toFixed(1)}
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

          {!usuario && <p>Entre na sua conta para ver suas reservas.</p>}
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
              <p>Entre na sua conta para ver os livros que você favoritou.</p>
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
                <span>📘</span>
              )}
            </div>

            <div className="livro-dados">
              <span className="book-category">
                {livroAberto.materia || "Sem matéria"}
              </span>
              <h1>{livroAberto.titulo}</h1>
              <p className="livro-autores">
                {(livroAberto.autores || []).join(", ") || "Autor desconhecido"}
              </p>

              <div className="livro-atributos">
                <div>
                  <small>Ano de lançamento</small>
                  <strong>{livroAberto.anoLancamento || "—"}</strong>
                </div>
                <div>
                  <small>Páginas</small>
                  <strong>{livroAberto.numeroPagina || 0}</strong>
                </div>
                <div>
                  <small>Matéria</small>
                  <strong>{livroAberto.materia || "Sem matéria"}</strong>
                </div>
                <div>
                  <small>Preço</small>
                  <strong>{formatarPreco(livroAberto.preco)}</strong>
                </div>
                <div>
                  <small>Avaliação</small>
                  <strong>
                    ★ {Number(livroAberto.avalliacao || 0).toFixed(1)}
                  </strong>
                </div>
                <div>
                  <small>Estoque</small>
                  <strong>{formatarEstoque(livroAberto.estoque)}</strong>
                </div>
              </div>

              <div className="livro-acoes">
                {!ehFuncionario && (
                  <button
                    type="button"
                    className={"favoritar-button" + (favoritado ? " ativo" : "")}
                    onClick={() => alternarFavorito(livroAberto)}
                    disabled={favoritoEmEdicao === livroAberto.id}
                  >
                    {favoritado ? "♥ Remover dos favoritos" : "♡ Favoritar"}
                  </button>
                )}
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
                      Number(livroAberto.estoque || 0) <= 0
                    }
                  >
                    {Number(livroAberto.estoque || 0) <= 0
                      ? "Sem estoque"
                      : "Reservar livro"}
                  </button>
                )}
                {ehFuncionario && (
                  <button
                    type="button"
                    className="perigo"
                    onClick={() => deletarLivro(livroAberto)}
                    disabled={salvando}
                  >
                    Excluir livro
                  </button>
                )}
              </div>
            </div>
          </div>

          {ehFuncionario && (
            <div className="livro-edicao">
              <span className="section-label">EDITAR</span>
              <h2>Atualizar informações</h2>

              <form className="modal-form" onSubmit={atualizarLivro}>
                <label>
                  Título
                  <input
                    type="text"
                    value={formEdicao.titulo || ""}
                    onChange={(e) =>
                      setFormEdicao({ ...formEdicao, titulo: e.target.value })
                    }
                  />
                </label>
                <label>
                  Matéria
                  <select
                    value={nomeDaMateria(formEdicao.materia)}
                    onChange={(e) =>
                      setFormEdicao({ ...formEdicao, materia: e.target.value })
                    }
                  >
                    <option value="">Sem matéria</option>
                    {materias.map((materia) => (
                      <option key={materia.nome} value={materia.nome}>
                        {materia.rotulo}
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  Ano de lançamento
                  <input
                    type="text"
                    value={formEdicao.anoLancamento || ""}
                    onChange={(e) =>
                      setFormEdicao({
                        ...formEdicao,
                        anoLancamento: e.target.value,
                      })
                    }
                  />
                </label>
                <label>
                  Páginas
                  <input
                    type="number"
                    min="0"
                    value={formEdicao.numeroPagina || 0}
                    onChange={(e) =>
                      setFormEdicao({
                        ...formEdicao,
                        numeroPagina: e.target.value,
                      })
                    }
                  />
                </label>
                <label>
                  Preço
                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    value={formEdicao.preco || 0}
                    onChange={(e) =>
                      setFormEdicao({ ...formEdicao, preco: e.target.value })
                    }
                  />
                </label>
                <label>
                  Estoque
                  <input
                    type="number"
                    min="0"
                    step="1"
                    value={
                      formEdicao.estoque === undefined ||
                      formEdicao.estoque === null
                        ? 0
                        : formEdicao.estoque
                    }
                    onChange={(e) =>
                      setFormEdicao({ ...formEdicao, estoque: e.target.value })
                    }
                  />
                </label>
                <label>
                  Avaliação
                  <input
                    type="number"
                    min="0"
                    max="5"
                    step="0.1"
                    value={formEdicao.avalliacao || 0}
                    onChange={(e) =>
                      setFormEdicao({ ...formEdicao, avalliacao: e.target.value })
                    }
                  />
                </label>

                <div className="modal-actions">
                  <button type="submit" disabled={salvando}>
                    {salvando ? "Salvando..." : "Atualizar livro"}
                  </button>
                </div>
              </form>
            </div>
          )}
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
            BIBLIOTECA VIRTUAL
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
            <span className="search-icon">🔎</span>
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

          {!carregando && livrosExibidos.map(cartaoLivro)}
        </div>
      </section>

      {modalCadastroLivro}
      {modalAutenticacao}
    </div>
  );
}

export default App;
