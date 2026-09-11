/* eslint-disable jsx-a11y/anchor-is-valid */
import { useCallback, useEffect, useMemo, useState } from "react";
import "./App.css";
import { livrosApi, usuariosApi } from "./services/api";

// Icone exibido em cada categoria, por nome da constante do enum Materia.
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

const FORM_LIVRO_VAZIO = { titulo: "", materia: "" };
const FORM_USUARIO_VAZIO = { nome: "", email: "", senha: "" };

function App() {
  // Acervo completo: e sempre a base dos destaques e do ranking, por isso a
  // busca e o filtro por materia nao mexem nesta lista.
  const [livros, setLivros] = useState([]);
  const [materias, setMaterias] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState(null);
  const [aviso, setAviso] = useState(null);

  const [busca, setBusca] = useState("");
  // filtro.tipo: "todos" | "busca" | "materia" | "favoritos"
  const [filtro, setFiltro] = useState({ tipo: "todos" });
  const [resultado, setResultado] = useState([]);

  const [usuarioId, setUsuarioId] = useState(null);
  const [favoritos, setFavoritos] = useState([]);
  const [favoritoEmEdicao, setFavoritoEmEdicao] = useState(null);

  const [livroSelecionado, setLivroSelecionado] = useState(null);
  const [formLivro, setFormLivro] = useState(FORM_LIVRO_VAZIO);
  const [mostrarCadastroLivro, setMostrarCadastroLivro] = useState(false);
  const [mostrarCadastroUsuario, setMostrarCadastroUsuario] = useState(false);
  const [formUsuario, setFormUsuario] = useState(FORM_USUARIO_VAZIO);
  const [salvando, setSalvando] = useState(false);

  const idsFavoritos = useMemo(
    () => new Set(favoritos.map((livro) => livro.id)),
    [favoritos]
  );

  // O back-end serializa a materia pelo rotulo ("Veterinária"), mas as rotas e
  // os formularios trabalham com o nome da constante ("VETERINARIA").
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

  // Ainda nao existe tela de login, entao o primeiro usuario cadastrado e
  // usado como usuario logado para os favoritos.
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

  const alternarFavorito = async (livro) => {
    if (!usuarioId) {
      setErro("Cadastre um usuário para poder favoritar livros.");
      setMostrarCadastroUsuario(true);
      return;
    }
    // Evita que um clique duplo dispare dois POST e favorite o mesmo livro duas vezes.
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

      // Relê a lista no servidor para que a tela reflita exatamente o que foi gravado.
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
        formLivro.materia
      );
      setFormLivro(FORM_LIVRO_VAZIO);
      setMostrarCadastroLivro(false);
      setAviso(`"${livro.titulo}" foi cadastrado.`);
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
    const livro = livroSelecionado;

    try {
      setSalvando(true);
      setErro(null);
      const atualizado = await livrosApi.atualizar(livro.id, {
        titulo: livro.titulo,
        anoLancamento: livro.anoLancamento,
        numeroPagina: Number(livro.numeroPagina) || 0,
        materia: nomeDaMateria(livro.materia) || null,
        avaliacao: Number(livro.avalliacao) || 0,
      });
      setLivroSelecionado(null);
      setAviso(`"${atualizado.titulo}" foi atualizado.`);
      await Promise.all([carregarLivros(), carregarMaterias()]);
      if (usuarioId) {
        setFavoritos(await usuariosApi.listarFavoritos(usuarioId));
      }
      limparFiltro();
    } catch (e) {
      setErro("Não foi possível atualizar o livro.");
      console.error(e);
    } finally {
      setSalvando(false);
    }
  };

  const deletarLivro = async (livro) => {
    if (!window.confirm(`Remover "${livro.titulo}" do acervo?`)) {
      return;
    }

    try {
      setSalvando(true);
      setErro(null);
      await livrosApi.deletar(livro.id);
      setLivroSelecionado(null);
      setAviso(`"${livro.titulo}" foi removido do acervo.`);
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
      ? `Resultado para "${filtro.termo}"`
      : filtro.tipo === "materia"
      ? `Livros de ${filtro.rotulo}`
      : "Livros em destaque";

  const rotuloDaLista =
    filtro.tipo === "todos" ? "RECOMENDADOS" : "RESULTADO DA BUSCA";

  // O ranking usa sempre o acervo completo, nunca o resultado da busca.
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

  return (
    <div className="app">
      {/* HEADER */}
      <header className="header">
        <div className="logo">Pallanthir</div>
        <nav className="nav">
          <button
            type="button"
            className={"nav-link" + (filtro.tipo === "todos" ? " active" : "")}
            onClick={limparFiltro}
          >
            Início
          </button>
          <a href="#livros">Livros</a>
          <a href="#categorias">Categorias</a>
          <a href="#plano">Plano de estudos</a>
          <button
            type="button"
            className={
              "nav-link" + (filtro.tipo === "favoritos" ? " active" : "")
            }
            onClick={() =>
              setFiltro((atual) =>
                atual.tipo === "favoritos" ? { tipo: "todos" } : { tipo: "favoritos" }
              )
            }
          >
            Favoritos ♡ {favoritos.length > 0 && `(${favoritos.length})`}
          </button>
        </nav>
        <button
          className="login-button"
          onClick={() => setMostrarCadastroUsuario(true)}
        >
          {usuarioId ? "Minha conta" : "Entrar"}
        </button>
      </header>

      {/* HERO */}
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

          {/* PESQUISA */}
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

      {/* CATEGORIAS */}
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

      {/* LIVROS */}
      <section className="section" id="livros">
        <div className="section-header">
          <div>
            <span className="section-label">{rotuloDaLista}</span>
            <h2>{tituloDaLista}</h2>
          </div>
          <div className="section-actions">
            <button
              type="button"
              className="link-button"
              onClick={() => setMostrarCadastroLivro((atual) => !atual)}
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

        {mostrarCadastroLivro && (
          <form className="painel-form" onSubmit={cadastrarLivro}>
            <input
              type="text"
              placeholder="Título do livro"
              value={formLivro.titulo}
              onChange={(e) =>
                setFormLivro({ ...formLivro, titulo: e.target.value })
              }
            />
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
            <button type="submit" disabled={salvando}>
              {salvando ? "Salvando..." : "Cadastrar"}
            </button>
          </form>
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
                <div className="book-card" key={livro.id}>
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
                      onClick={() => alternarFavorito(livro)}
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
                      <strong>{livro.numeroPagina} páginas</strong>
                      <button
                        type="button"
                        onClick={() => setLivroSelecionado({ ...livro })}
                      >
                        Ver detalhes
                      </button>
                    </div>
                  </div>
                </div>
              ))}
          </div>

          {/* MAIS BEM AVALIADOS */}
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
                <div className="rank" key={livro.id}>
                  <span className="number">
                    {String(indice + 1).padStart(2, "0")}
                  </span>
                  <div>
                    <strong>{livro.titulo}</strong>
                    <small>
                      nota {Number(livro.avalliacao || 0).toFixed(1)}
                    </small>
                  </div>
                </div>
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

      {/* PLANO DE ESTUDOS */}
      <section className="study-section" id="plano">
        <div className="study-text">
          <span className="section-label">PLANO DE ESTUDOS</span>
          <h2>
            Transforme sua leitura
            <strong> em conhecimento.</strong>
          </h2>
          <p>
            Crie um plano de estudos personalizado e receba sugestões de livros
            de acordo com seus objetivos.
          </p>
          <button
            type="button"
            onClick={() => {
              setMostrarCadastroLivro(true);
              document
                .getElementById("livros")
                ?.scrollIntoView({ behavior: "smooth" });
            }}
          >
            Criar meu plano →
          </button>
        </div>

        <div className="study-books">
          📕
          <span>📗</span>
          <span>📘</span>
        </div>
      </section>

      {/* FOOTER */}
      <footer className="footer">
        <div className="footer-logo">Pallanthir</div>
        <div>
          <h4>Sobre</h4>
          <a href="#">Nossa história</a>
          <a href="#">Como funciona</a>
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
          <a href="#">Perguntas frequentes</a>
          <a href="#">Privacidade</a>
        </div>
        <div>
          <h4>Contato</h4>
          <a href="#">contato@pallanthir.com</a>
          <a href="#">(11) 99999-9999</a>
        </div>
      </footer>

      {/* DETALHES / EDICAO DO LIVRO */}
      {livroSelecionado && (
        <div className="modal" onClick={() => setLivroSelecionado(null)}>
          <div className="modal-box" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{livroSelecionado.titulo}</h3>
              <button type="button" onClick={() => setLivroSelecionado(null)}>
                ✕
              </button>
            </div>
            <p className="modal-sub">
              {(livroSelecionado.autores || []).join(", ")} ·{" "}
              {livroSelecionado.anoLancamento}
            </p>

            <form className="modal-form" onSubmit={atualizarLivro}>
              <label>
                Título
                <input
                  type="text"
                  value={livroSelecionado.titulo || ""}
                  onChange={(e) =>
                    setLivroSelecionado({
                      ...livroSelecionado,
                      titulo: e.target.value,
                    })
                  }
                />
              </label>
              <label>
                Matéria
                <select
                  value={nomeDaMateria(livroSelecionado.materia)}
                  onChange={(e) =>
                    setLivroSelecionado({
                      ...livroSelecionado,
                      materia: e.target.value,
                    })
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
                Páginas
                <input
                  type="number"
                  min="0"
                  value={livroSelecionado.numeroPagina || 0}
                  onChange={(e) =>
                    setLivroSelecionado({
                      ...livroSelecionado,
                      numeroPagina: e.target.value,
                    })
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
                  value={livroSelecionado.avalliacao || 0}
                  onChange={(e) =>
                    setLivroSelecionado({
                      ...livroSelecionado,
                      avalliacao: e.target.value,
                    })
                  }
                />
              </label>

              <div className="modal-actions">
                <button
                  type="button"
                  className="perigo"
                  onClick={() => deletarLivro(livroSelecionado)}
                  disabled={salvando}
                >
                  Excluir
                </button>
                <button type="submit" disabled={salvando}>
                  {salvando ? "Salvando..." : "Salvar alterações"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* CADASTRO DE USUARIO */}
      {mostrarCadastroUsuario && (
        <div className="modal" onClick={() => setMostrarCadastroUsuario(false)}>
          <div className="modal-box" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{usuarioId ? "Nova conta" : "Criar conta"}</h3>
              <button
                type="button"
                onClick={() => setMostrarCadastroUsuario(false)}
              >
                ✕
              </button>
            </div>
            <p className="modal-sub">
              Os favoritos usam o primeiro usuário cadastrado enquanto não há
              tela de login.
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
      )}
    </div>
  );
}

export default App;
