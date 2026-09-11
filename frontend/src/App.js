/* eslint-disable jsx-a11y/anchor-is-valid */
import { useCallback, useEffect, useState } from "react";
import "./App.css";
import { livrosApi, usuariosApi } from "./services/api";

function App() {
  const [livros, setLivros] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState(null);
  const [busca, setBusca] = useState("");
  const [usuarioId, setUsuarioId] = useState(null);
  const [favoritos, setFavoritos] = useState([]);
  const [verFavoritos, setVerFavoritos] = useState(false);

  const idsFavoritos = new Set(favoritos.map((livro) => livro.id));

  const carregarLivros = useCallback(async () => {
    try {
      setCarregando(true);
      setErro(null);
      const dados = await livrosApi.listar();
      setLivros(dados);
    } catch (e) {
      setErro("Não foi possível carregar os livros. Verifique se o back-end está no ar.");
      console.error(e);
    } finally {
      setCarregando(false);
    }
  }, []);

  // Ainda nao existe tela de login, entao o primeiro usuario cadastrado e
  // usado como usuario logado para os favoritos.
  const carregarUsuario = useCallback(async () => {
    try {
      const usuarios = await usuariosApi.listar();
      if (usuarios.length === 0) {
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
    carregarUsuario();
  }, [carregarLivros, carregarUsuario]);

  const alternarFavorito = async (livro) => {
    if (!usuarioId) {
      setErro("Cadastre um usuario para poder favoritar livros.");
      return;
    }

    try {
      if (idsFavoritos.has(livro.id)) {
        await usuariosApi.desfavoritar(usuarioId, livro.id);
        setFavoritos((atuais) => atuais.filter((f) => f.id !== livro.id));
      } else {
        await usuariosApi.favoritar(usuarioId, livro.id);
        setFavoritos((atuais) => [...atuais, livro]);
      }
    } catch (e) {
      setErro("Nao foi possivel atualizar os favoritos.");
      console.error(e);
    }
  };

  const pesquisar = async (evento) => {
    evento.preventDefault();

    if (!busca.trim()) {
      carregarLivros();
      return;
    }

    try {
      setCarregando(true);
      setErro(null);
      const livro = await livrosApi.buscarPorTitulo(busca.trim());
      setLivros(livro ? [livro] : []);
    } catch (e) {
      setLivros([]);
      setErro("Nenhum livro encontrado para: " + busca);
      console.error(e);
    } finally {
      setCarregando(false);
    }
  };

  const livrosExibidos = verFavoritos ? favoritos : livros;

  const maisAvaliados = [...livros]
    .sort((a, b) => b.avalliacao - a.avalliacao)
    .slice(0, 3);

  return (
    <div className="app">
      {/* HEADER */}
      <header className="header">
        <div className="logo">Pallanthir</div>
        <nav className="nav">
          <a href="#" className="active">Início</a>
          <a href="#">Livros</a>
          <a href="#">Categorias</a>
          <a href="#">Plano de estudos</a>
          <button
            type="button"
            className={"nav-link" + (verFavoritos ? " active" : "")}
            onClick={() => setVerFavoritos((atual) => !atual)}
          >
            Favoritos ♡ {favoritos.length > 0 && `(${favoritos.length})`}
          </button>
        </nav>
        <button className="login-button">Entrar</button>
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
            Explore livros, descubra novos conhecimentos e
            encontre conteúdos para transformar seus estudos.
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
      <section className="section">
        <div className="section-header">
          <div>
            <span className="section-label">EXPLORE</span>
            <h2>Categorias</h2>
          </div>
          <a href="#">Ver todas →</a>
        </div>

        <div className="categories">
          <div className="category">
            <div className="category-icon">💻</div>
            <h3>Tecnologia</h3>
            <p>1.240 livros</p>
          </div>
          <div className="category">
            <div className="category-icon">❤️</div>
            <h3>Romance</h3>
            <p>980 livros</p>
          </div>
          <div className="category">
            <div className="category-icon">📚</div>
            <h3>Ficção</h3>
            <p>1.520 livros</p>
          </div>
          <div className="category">
            <div className="category-icon">🏛️</div>
            <h3>História</h3>
            <p>760 livros</p>
          </div>
          <div className="category">
            <div className="category-icon">🧠</div>
            <h3>Desenvolvimento</h3>
            <p>640 livros</p>
          </div>
        </div>
      </section>

      {/* LIVROS */}
      <section className="section">
        <div className="section-header">
          <div>
            <span className="section-label">RECOMENDADOS</span>
            <h2>Livros em destaque</h2>
          </div>
          <a href="#">Ver todos →</a>
        </div>

        <div className="main-content">
          <div className="books">
            {carregando && <p>Carregando livros...</p>}
            {!carregando && erro && <p className="erro">{erro}</p>}
            {!carregando && !erro && livrosExibidos.length === 0 && (
              <p>
                {verFavoritos
                  ? "Voce ainda nao favoritou nenhum livro."
                  : "Nenhum livro cadastrado ainda."}
              </p>
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
                      <button>Ver detalhes</button>
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
              {maisAvaliados.map((livro, indice) => (
                <div className="rank" key={livro.id}>
                  <span className="number">
                    {String(indice + 1).padStart(2, "0")}
                  </span>
                  <div>
                    <strong>{livro.titulo}</strong>
                    <small>nota {Number(livro.avalliacao || 0).toFixed(1)}</small>
                  </div>
                </div>
              ))}
            </div>

            <button className="ranking-button">Ver ranking completo →</button>
          </aside>
        </div>
      </section>

      {/* PLANO DE ESTUDOS */}
      <section className="study-section">
        <div className="study-text">
          <span className="section-label">PLANO DE ESTUDOS</span>
          <h2>
            Transforme sua leitura
            <strong> em conhecimento.</strong>
          </h2>
          <p>
            Crie um plano de estudos personalizado e receba
            sugestões de livros de acordo com seus objetivos.
          </p>
          <button>Criar meu plano →</button>
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
          <a href="#">Tecnologia</a>
          <a href="#">Romance</a>
          <a href="#">Ficção</a>
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
    </div>
  );
}

export default App;
