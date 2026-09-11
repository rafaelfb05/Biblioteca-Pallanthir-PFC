import { useCallback, useEffect, useMemo, useState } from "react";
import "./App.css";
import { livrosApi, usuariosApi } from "./services/api";

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

const FORM_LIVRO_VAZIO = { titulo: "", materia: "", preco: "" };
const FORM_USUARIO_VAZIO = { nome: "", email: "", senha: "" };

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

  const [busca, setBusca] = useState("");
  const [filtro, setFiltro] = useState({ tipo: "todos" });
  const [resultado, setResultado] = useState([]);

  const [usuarioId, setUsuarioId] = useState(null);
  const [favoritos, setFavoritos] = useState([]);
  const [favoritoEmEdicao, setFavoritoEmEdicao] = useState(null);

  const [livroAbertoId, setLivroAbertoId] = useState(null);
  const [formEdicao, setFormEdicao] = useState(null);
  const [formLivro, setFormLivro] = useState(FORM_LIVRO_VAZIO);
  const [mostrarCadastroLivro, setMostrarCadastroLivro] = useState(false);
  const [mostrarCadastroUsuario, setMostrarCadastroUsuario] = useState(false);
  const [formUsuario, setFormUsuario] = useState(FORM_USUARIO_VAZIO);
  const [salvando, setSalvando] = useState(false);

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

  const carregarUsuario = useCallback(async () => {
    try {
      const usuarios = await usuariosApi.listar();
      if (usuarios.length === 0) {
        setUsuarioId(null);
        setFavoritos([]);
        return;
      }
      const usuario = usuarios[0];
      setUsuarioId(usuario.id);
      setFavoritos(await usuariosApi.listarFavoritos(usuario.id));
    } catch (e) {
      console.error(e);
    }
  }, []);

  useEffect(() => {
    carregarLivros();
    carregarMaterias();
    carregarUsuario();
  }, [carregarLivros, carregarMaterias, carregarUsuario]);

  const limparFiltro = () => {
    setFiltro({ tipo: "todos" });
    setResultado([]);
    setBusca("");
    setErro(null);
  };

  const abrirLivro = (livro) => {
    setLivroAbertoId(livro.id);
    setFormEdicao({ ...livro });
    setErro(null);
    window.scrollTo({ top: 0 });
  };

  const voltarParaInicio = () => {
    setLivroAbertoId(null);
    setFormEdicao(null);
    setErro(null);
    window.scrollTo({ top: 0 });
  };

  const alternarFavorito = async (livro) => {
    if (!usuarioId) {
      setErro("Cadastre um usuário para poder favoritar livros.");
      setMostrarCadastroUsuario(true);
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
        Number(String(formLivro.preco).replace(",", ".")) || 0
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
      await usuariosApi.cadastrar(formUsuario);
      setFormUsuario(FORM_USUARIO_VAZIO);
      setMostrarCadastroUsuario(false);
      setAviso("Usuário cadastrado. Agora você pode favoritar livros.");
      await carregarUsuario();
    } catch (e) {
      setErro("Não foi possível cadastrar o usuário. O e-mail já pode existir.");
      console.error(e);
    } finally {
      setSalvando(false);
    }
  };

  const livrosExibidos =
    filtro.tipo === "favoritos"
      ? favoritos
      : filtro.tipo === "todos"
      ? livros
      : resultado;

  const tituloDaLista =
    filtro.tipo === "favoritos"
      ? "Meus favoritos"
      : filtro.tipo === "busca"
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

  const mensagemListaVazia =
    filtro.tipo === "favoritos"
      ? "Você ainda não favoritou nenhum livro."
      : filtro.tipo === "todos"
      ? "Nenhum livro cadastrado ainda."
      : "Nenhum livro encontrado para este filtro.";

  const livroAberto =
    livroAbertoId === null
      ? null
      : [...livros, ...favoritos, ...resultado].find(
          (livro) => livro.id === livroAbertoId
        ) || formEdicao;

  const abrirCadastroLivro = () => {
    setLivroAbertoId(null);
    setFormEdicao(null);
    setErro(null);
    setMostrarCadastroLivro(true);
  };

  const cabecalho = (
    <header className="header">
      <button type="button" className="logo" onClick={voltarParaInicio}>
        Pallanthir
      </button>
      <nav className="nav">
        <button
          type="button"
          className={
            "nav-link" +
            (livroAbertoId === null && filtro.tipo === "todos" ? " active" : "")
          }
          onClick={() => {
            voltarParaInicio();
            limparFiltro();
          }}
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
          className="nav-link"
          onClick={() => {
            voltarParaInicio();
            document
              .getElementById("categorias")
              ?.scrollIntoView({ behavior: "smooth" });
          }}
        >
          Categorias
        </button>
        <button
          type="button"
          className={
            "nav-link" +
            (livroAbertoId === null && filtro.tipo === "favoritos"
              ? " active"
              : "")
          }
          onClick={() => {
            voltarParaInicio();
            setFiltro((atual) =>
              atual.tipo === "favoritos"
                ? { tipo: "todos" }
                : { tipo: "favoritos" }
            );
          }}
        >
          Favoritos ♡ {favoritos.length > 0 && "(" + favoritos.length + ")"}
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
        <button
          type="button"
          className="login-button"
          onClick={() => setMostrarCadastroUsuario(true)}
        >
          {usuarioId ? "Minha conta" : "Entrar"}
        </button>
      </div>
    </header>
  );

  const rodape = (
    <footer className="footer">
      <div className="footer-logo">Pallanthir</div>
      <div>
        <h4>Sobre</h4>
        <button type="button" className="link-button">Nossa história</button>
        <button type="button" className="link-button">Como funciona</button>
      </div>
      <div>
        <h4>Categorias</h4>
        {materias.slice(0, 3).map((materia) => (
          <button
            type="button"
            className="link-button"
            key={materia.nome}
            onClick={() => filtrarPorMateria(materia)}
          >
            {materia.rotulo}
          </button>
        ))}
      </div>
      <div>
        <h4>Ajuda</h4>
        <button type="button" className="link-button">Perguntas frequentes</button>
        <button type="button" className="link-button">Privacidade</button>
      </div>
      <div>
        <h4>Contato</h4>
        <button type="button" className="link-button">contato@pallanthir.com</button>
        <button type="button" className="link-button">(11) 99999-9999</button>
      </div>
    </footer>
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

  const modalCadastroUsuario = mostrarCadastroUsuario && (
    <div className="modal" onClick={() => setMostrarCadastroUsuario(false)}>
      <div className="modal-box" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3>{usuarioId ? "Nova conta" : "Criar conta"}</h3>
          <button type="button" onClick={() => setMostrarCadastroUsuario(false)}>
            ✕
          </button>
        </div>
        <p className="modal-sub">
          Os favoritos usam o primeiro usuário cadastrado enquanto não há tela
          de login.
        </p>

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

          <div className="modal-actions">
            <button type="submit" disabled={salvando}>
              {salvando ? "Salvando..." : "Cadastrar"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );

  if (livroAberto && formEdicao) {
    const favoritado = idsFavoritos.has(livroAberto.id);

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
                  <small>Código</small>
                  <strong>#{livroAberto.id}</strong>
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

        {rodape}
        {modalCadastroLivro}
        {modalCadastroUsuario}
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

      <section className="section" id="categorias">
        <div className="section-header">
          <div>
            <span className="section-label">EXPLORE</span>
            <h2>Categorias</h2>
          </div>
          <button type="button" className="link-button" onClick={limparFiltro}>
            Ver todas →
          </button>
        </div>

        <div className="categories">
          {materias.length === 0 && <p>Carregando categorias...</p>}
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

            {!carregando &&
              livrosExibidos.map((livro) => (
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
                      className={
                        "favorite" + (idsFavoritos.has(livro.id) ? " ativo" : "")
                      }
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
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          abrirLivro(livro);
                        }}
                      >
                        Ver detalhes
                      </button>
                    </div>
                  </div>
                </div>
              ))}
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

      {rodape}
      {modalCadastroLivro}
      {modalCadastroUsuario}
    </div>
  );
}

export default App;
