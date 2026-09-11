import "./App.css";
function App() {
 const livros = [
   {
     titulo: "Java: Como Programar",
     autor: "Paul Deitel",
     categoria: "Programação",
     preco: "R$ 89,90",
     emoji: "☕",
   },
   {
     titulo: "Clean Code",
     autor: "Robert C. Martin",
     categoria: "Tecnologia",
     preco: "R$ 79,90",
     emoji: "💻",
   },
   {
     titulo: "O Senhor dos Anéis",
     autor: "J. R. R. Tolkien",
     categoria: "Fantasia",
     preco: "R$ 69,90",
     emoji: "📖",
   },
   {
     titulo: "Hábitos Atômicos",
     autor: "James Clear",
     categoria: "Desenvolvimento",
     preco: "R$ 59,90",
     emoji: "🧠",
   },
 ];
 return (
<div className="app">
     {/* HEADER */}
<header className="header">
<div className="logo">
         Pallanthir
</div>
<nav className="nav">
<a href="#" className="active">Início</a>
<a href="#">Livros</a>
<a href="#">Categorias</a>
<a href="#">Plano de estudos</a>
<a href="#">Favoritos ♡</a>
</nav>
<button className="login-button">
         Entrar
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
           Explore livros, descubra novos conhecimentos e
           encontre conteúdos para transformar seus estudos.
</p>

         {/* PESQUISA */}
<div className="search">
<span className="search-icon">
             🔎
</span>
<input
             type="text"
             placeholder="Pesquise por título, autor ou categoria..."
           />
<button>
             Pesquisar
</button>
</div>
</div>
</section>

     {/* CATEGORIAS */}
<section className="section">
<div className="section-header">
<div>
<span className="section-label">
             EXPLORE
</span>
<h2>
             Categorias
</h2>
</div>
<a href="#">
           Ver todas →
</a>
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
<span className="section-label">
             RECOMENDADOS
</span>
<h2>
             Livros em destaque
</h2>
</div>
<a href="#">
           Ver todos →
</a>
</div>

<div className="main-content">
<div className="books">
           {livros.map((livro, index) => (
<div className="book-card" key={index}>
<div className="book-cover">
<span>
                   {livro.emoji}
</span>
<button className="favorite">
                   ♡
</button>
</div>
<div className="book-info">
<span className="book-category">
                   {livro.categoria}
</span>
<h3>
                   {livro.titulo}
</h3>
<p>
                   {livro.autor}
</p>
<div className="rating">
                   ★★★★★
<small> 4.8</small>
</div>
<div className="book-footer">
<strong>
                     {livro.preco}
</strong>
<button>
                     Ver detalhes
</button>
</div>
</div>
</div>
           ))}
</div>

         {/* MAIS VENDIDOS */}
<aside className="bestsellers">
<div className="best-title">
<span>♛</span>
<h3>Mais vendidos</h3>
</div>
<p className="best-subtitle">
             Os favoritos deste mês
</p>

<div className="ranking">
<div className="rank">
<span className="number">01</span>
<div>
<strong>Java: Como Programar</strong>
<small>152 vendas</small>
</div>
</div>

<div className="rank">
<span className="number">02</span>
<div>
<strong>Clean Code</strong>
<small>127 vendas</small>
</div>
</div>

<div className="rank">
<span className="number">03</span>
<div>
<strong>O Senhor dos Anéis</strong>
<small>103 vendas</small>
</div>
</div>
</div>

<button className="ranking-button">
             Ver ranking completo →
</button>
</aside>
</div>
</section>

     {/* PLANO DE ESTUDOS */}
<section className="study-section">
<div className="study-text">
<span className="section-label">
           PLANO DE ESTUDOS
</span>
<h2>
           Transforme sua leitura
<strong> em conhecimento.</strong>
</h2>
<p>
           Crie um plano de estudos personalizado e receba
           sugestões de livros de acordo com seus objetivos.
</p>
<button>
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
<div className="footer-logo">
         Pallanthir
</div>
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