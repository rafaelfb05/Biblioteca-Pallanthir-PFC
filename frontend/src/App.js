import { useCallback, useEffect, useMemo, useState } from "react";
import "./App.css";
import { livrosApi, reservasApi, usuariosApi } from "./services/api";

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
const FORM_USUARIO_VAZIO = { nome: "", email: "", senha: "" };
const FORM_LOGIN_VAZIO = { email: "", senha: "" };
const CHAVE_SESSAO = "pallanthir:usuario";

const formatarEstoque = (quantidade) => {
  const total = Number(quantidade || 0);
  if (total <= 0) {
    return "Sem estoque";
  }
  return total + (total === 1 ? " unidade" : " unidades");
};

const ROTULO_STATUS_RESERVA = {
  RESERVADO: "Reservado",
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
  const [formEdicao, setFormEdicao] = useState(null);
  const [formLivro, setFormLivro] = useState(FORM_LIVRO_VAZIO);
  const [mostrarCadastroLivro, setMostrarCadastroLivro] = useState(false);
  const [mostrarAutenticacao, setMostrarAutenticacao] = useState(false);
  const [modoAutenticacao, setModoAutenticacao] = useState("login");
  const [formUsuario, setFormUsuario] = useState(FORM_USUARIO_VAZIO);
  const [formLogin, setFormLogin] = useState(FORM_LOGIN_VAZIO);
  const [salvando, setSalvando] = useState(false);

  const usuarioId = usuario ? usuario.id : null;

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

  const restaurarSessao = useCallback(async () => {
    try {
      const salvo = localStorage.getItem(CHAVE_SESSAO);
      if (!salvo) {
        return;
      }
      const sessao = JSON.parse(salvo);
      setUsuario(sessao);
      await Promise.all([
        carregarFavoritos(sessao.id),
        carregarReservas(sessao.id),
      ]);
    } catch (e) {
      console.error(e);
    }
  }, [carregarFavoritos, carregarReservas]);

  useEffect(() => {
    carregarLivros();
    carregarMaterias();
    restaurarSessao();
  }, [carregarLivros, carregarMaterias, restaurarSessao]);

  const abrirAutenticacao = (modo) => {
    setModoAutenticacao(modo || "login");
    setErro(null);
    setMostrarAutenticacao(true);
  };

  const fecharAutenticacao = () => {
    setMostrarAutenticacao(false);
    setErro(null);
  };

  const iniciarSessao = async (autenticado) => {
    const sessao = {
      id: autenticado.id,
      nome: autenticado.nome,
      email: autenticado.email,
    };
    try {
      localStorage.setItem(CHAVE_SESSAO, JSON.stringify(sessao));
    } catch (e) {
      console.error(e);
    }
    setUsuario(sessao);
    setMostrarAutenticacao(false);
    await Promise.all([
      carregarFavoritos(sessao.id),
      carregarReservas(sessao.id),
    ]);
  };

  const entrar = async (evento) => {
    evento.preventDefault();

    try {
      setSalvando(true);
      setErro(null);
      const autenticado = await usuariosApi.login(
        formLogin.email.trim(),
        formLogin.senha
      );
      setFormLogin(FORM_LOGIN_VAZIO);
      await iniciarSessao(autenticado);
      setAviso("Bem-vindo(a), " + autenticado.nome + "!");
    } catch (e) {
      setErro("E-mail ou senha incorretos.");
      console.error(e);
    } finally {
      setSalvando(false);
    }
  };

  const sair = () => {
    try {
      localStorage.removeItem(CHAVE_SESSAO);
    } catch (e) {
      console.error(e);
    }
    setUsuario(null);
    setFavoritos([]);
    setReservas([]);
    setPagina((atual) =>
      atual === "favoritos" || atual === "reservas" ? "inicio" : atual
    );
    setAviso("Você saiu da sua conta.");
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

  const irParaFavoritos = () => {
    setPagina("favoritos");
    setLivroAbertoId(null);
    setFormEdicao(null);
    setErro(null);
    window.scrollTo({ top: 0 });
  };

  const irParaMaterias = () => {
    setPagina("materias");
    setLivroAbertoId(null);
    setFormEdicao(null);
    setErro(null);
    window.scrollTo({ top: 0 });
  };

  const irParaReservas = async () => {
    setPagina("reservas");
    setLivroAbertoId(null);
    setFormEdicao(null);
    setErro(null);
    window.scrollTo({ top: 0 });
    if (usuarioId) {
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
      setErro("Não foi possível atualizar os favoritos.");
      console.error(e);
    } finally {
      setFavoritoEmEdicao(null);
    }
  };

  const reservaAtivaDoLivro = useCallback(
    (livro) =>
      reservas.find(
        (reserva) =>
          reserva.status === "RESERVADO" && reserva.livroId === livro.id
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
      setErro("Não foi possível reservar o livro. Ele pode estar esgotado.");
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
      setErro("Não foi possível cancelar a reserva.");
      console.error(e);
    } finally {
      setReservaEmEdicao(null);
    }
  };

  const devolverLivro = async (reserva) => {
    try {
      setReservaEmEdicao(reserva.id);
      setErro(null);
      await reservasApi.devolver(reserva.id);
      setAviso("\"" + reserva.tituloLivro + "\" foi devolvido.");
      await Promise.all([carregarLivros(), carregarReservas(usuarioId)]);
    } catch (e) {
      setErro("Não foi possível registrar a devolução.");
      console.error(e);
    } finally {
      setReservaEmEdicao(null);
    }
  };

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
        "Não foi possível cadastrar o livro. Confira o título na API de livros."
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
      if (usuarioId) {
        setFavoritos(await usuariosApi.listarFavoritos(usuarioId));
      }
    } catch (e) {
      setErro("Não foi possível atualizar o livro.");
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
      await livrosApi.deletar(livro.id);
      setLivroAbertoId(null);
      setFormEdicao(null);
      setAviso('"' + livro.titulo + '" foi removido do acervo.');
      await Promise.all([carregarLivros(), carregarMaterias()]);
      if (usuarioId) {
        setFavoritos(await usuariosApi.listarFavoritos(usuarioId));
      }
      limparFiltro();
    } catch (e) {
      setErro("Não foi possível remover o livro.");
      console.error(e);
    } finally {
      setSalvando(false);
    }
  };

  const cadastrarUsuario = async (evento) => {
    evento.preventDefault();

    try {
      setSalvando(true);
      setErro(null);
      const criado = await usuariosApi.cadastrar({
        ...formUsuario,
        email: formUsuario.email.trim(),
      });
      setFormUsuario(FORM_USUARIO_VAZIO);
      await iniciarSessao(criado);
      setAviso("Conta criada. Agora você pode favoritar livros.");
    } catch (e) {
      setErro("Não foi possível cadastrar o usuário. O e-mail já pode existir.");
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

  const maisAvaliados = useMemo(
    () =>
      [...livros]
        .sort((a, b) => (b.avalliacao || 0) - (a.avalliacao || 0))
        .slice(0, 3),
    [livros]
  );

  const reservasAtivas = useMemo(
    () => reservas.filter((reserva) => reserva.status === "RESERVADO"),
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
          <img src={livro.capaUrl} alt={"Capa de " + livro.titulo} />
        ) : (
          <span>📘</span>
        )}
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
          className={
            "nav-link" + (pagina === "favoritos" ? " active" : "")
          }
          onClick={irParaFavoritos}
        >
          Favoritos ♡ {favoritos.length > 0 && "(" + favoritos.length + ")"}
        </button>
        <button
          type="button"
          className={"nav-link" + (pagina === "reservas" ? " active" : "")}
          onClick={irParaReservas}
        >
          Reservas{" "}
          {reservasAtivas.length > 0 && "(" + reservasAtivas.length + ")"}
        </button>
      </nav>
      <div className="header-actions">
        <button
          type="button"
          className="cadastro-button"
          onClick={abrirCadastroLivro}
        >
          + Cadastrar livro
        </button>
        {usuario ? (
          <div className="sessao">
            <span className="sessao-nome">{usuario.nome}</span>
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

  const modalAutenticacao = mostrarAutenticacao && (
    <div className="modal" onClick={fecharAutenticacao}>
      <div className="modal-box" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3>{modoAutenticacao === "login" ? "Entrar" : "Criar conta"}</h3>
          <button type="button" onClick={fecharAutenticacao}>
            ✕
          </button>
        </div>

        <div className="abas-autenticacao">
          <button
            type="button"
            className={
              "aba-autenticacao" + (modoAutenticacao === "login" ? " ativa" : "")
            }
            onClick={() => {
              setModoAutenticacao("login");
              setErro(null);
            }}
          >
            Entrar
          </button>
          <button
            type="button"
            className={
              "aba-autenticacao" +
              (modoAutenticacao === "cadastro" ? " ativa" : "")
            }
            onClick={() => {
              setModoAutenticacao("cadastro");
              setErro(null);
            }}
          >
            Criar conta
          </button>
        </div>

        <p className="modal-sub">
          {modoAutenticacao === "login"
            ? "Informe seu e-mail e senha para acessar seus favoritos."
            : "Crie sua conta para salvar seus livros favoritos."}
        </p>

        {modoAutenticacao === "login" ? (
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
            <label>
              Senha
              <input
                type="password"
                required
                value={formLogin.senha}
                onChange={(e) =>
                  setFormLogin({ ...formLogin, senha: e.target.value })
                }
              />
            </label>

            {erro && <p className="erro">{erro}</p>}

            <div className="modal-actions">
              <button type="submit" disabled={salvando}>
                {salvando ? "Entrando..." : "Entrar"}
              </button>
            </div>
          </form>
        ) : (
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
            <label>
              Senha
              <input
                type="password"
                required
                value={formUsuario.senha}
                onChange={(e) =>
                  setFormUsuario({ ...formUsuario, senha: e.target.value })
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
        )}
      </div>
    </div>
  );

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
                        className="reservar-button"
                        onClick={() => devolverLivro(reserva)}
                        disabled={reservaEmEdicao === reserva.id}
                      >
                        Devolver
                      </button>
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
                <img
                  src={livroAberto.capaUrl}
                  alt={"Capa de " + livroAberto.titulo}
                />
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
                <button
                  type="button"
                  className={"favoritar-button" + (favoritado ? " ativo" : "")}
                  onClick={() => alternarFavorito(livroAberto)}
                  disabled={favoritoEmEdicao === livroAberto.id}
                >
                  {favoritado ? "♥ Remover dos favoritos" : "♡ Favoritar"}
                </button>
                {reservaAberta ? (
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
                <button
                  type="button"
                  className="perigo"
                  onClick={() => deletarLivro(livroAberto)}
                  disabled={salvando}
                >
                  Excluir livro
                </button>
              </div>
            </div>
          </div>

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
        </section>

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
            <button
              type="button"
              className="cadastro-button"
              onClick={abrirCadastroLivro}
            >
              + Cadastrar livro
            </button>
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

        <div className="main-content">
          <div className="books">
            {carregando && <p>Carregando livros...</p>}
            {!carregando && erro && <p className="erro">{erro}</p>}
            {!carregando && !erro && livrosExibidos.length === 0 && (
              <p>{mensagemListaVazia}</p>
            )}

            {!carregando && livrosExibidos.map(cartaoLivro)}
          </div>

          <aside className="bestsellers">
            <div className="best-title">
              <span>♛</span>
              <h3>Mais bem avaliados</h3>
            </div>
            <p className="best-subtitle">Os favoritos deste mês</p>

            <div className="ranking">
              {maisAvaliados.length === 0 && (
                <p className="best-subtitle">Nenhum livro no acervo ainda.</p>
              )}
              {maisAvaliados.map((livro, indice) => (
                <button
                  type="button"
                  className="rank"
                  key={livro.id}
                  onClick={() => abrirLivro(livro)}
                >
                  <span className="number">
                    {String(indice + 1).padStart(2, "0")}
                  </span>
                  <div>
                    <strong>{livro.titulo}</strong>
                    <small>
                      nota {Number(livro.avalliacao || 0).toFixed(1)}
                    </small>
                  </div>
                </button>
              ))}
            </div>

            <button
              type="button"
              className="ranking-button"
              onClick={() => {
                setBusca("");
                setResultado(
                  [...livros].sort(
                    (a, b) => (b.avalliacao || 0) - (a.avalliacao || 0)
                  )
                );
                setFiltro({ tipo: "busca", termo: "ranking completo" });
              }}
            >
              Ver ranking completo →
            </button>
          </aside>
        </div>
      </section>

      {modalCadastroLivro}
      {modalAutenticacao}
    </div>
  );
}

export default App;
