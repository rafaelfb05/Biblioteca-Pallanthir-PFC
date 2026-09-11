# Biblioteca Pallanthir

Um sistema de biblioteca criado para ajudar estudantes de diversas áreas.

Os usuários podem avaliar os livros lidos, e os livros com melhor avaliação são mais recomendados, ajudando alunos novos a encontrar mais facilmente as melhores opções para estudo. Também existe uma aba de favoritos para o estudante salvar os livros que mais gostar.

O sistema não oferece os livros digitalmente, apenas reservas e compras para buscar em uma biblioteca física.
 
---

## Tecnologias utilizadas

### Back-end

- **Java** — Linguagem de programação usada no back-end. Oferece recursos adequados para o desenvolvimento de aplicações robustas, escaláveis e orientadas a objetos.
- **Spring Boot** — Framework para desenvolvimento de aplicações Java. Fornece a infraestrutura para criação de aplicações web e APIs, facilitando a organização do projeto e oferecendo diversas dependências que agilizam o desenvolvimento.
- **Spring Security** — Utilizado para implementar os mecanismos de autenticação e autorização da aplicação, sendo responsável por auxiliar na segurança do sistema.
- **JWT (Json Web Token)** — Tecnologia utilizada no processo de autenticação e autorização dos usuários, permitindo a transmissão segura de informações relacionadas à sessão e à identidade do usuário.
- **BCrypt** — Algoritmo utilizado para gerar hashes das senhas dos usuários, permitindo armazená-las de forma mais segura no banco de dados.
### Front-end

- **HTML** — Linguagem de marcação usada no front-end. Funciona como o esqueleto do site.
- **CSS** — Linguagem responsável por definir a aparência visual e o estilo das páginas web escritas em HTML.
- **JavaScript** — Linguagem que adiciona dinamismo, interatividade e inteligência às páginas web, controlando ações e comportamentos dos elementos na tela.
- **React** — Biblioteca JavaScript para front-end baseada em uma arquitetura de componentes reutilizáveis, permitindo dividir a interface em partes independentes.
### Banco de dados

- **PostgreSQL** — Sistema gerenciador de banco de dados relacional, responsável pela persistência das informações da aplicação.
### Integrações externas

- **Google Books API** — API externa utilizada para obter informações dos livros adicionados ao sistema.
- **Mercado Pago API** — Utilizada para integrar o sistema ao serviço de pagamentos em compras ou aluguéis de livros.
### Hospedagem

- **Vercel** — Utilizada para hospedar e disponibilizar o front-end.
- **Render** — Utilizado para hospedar o back-end desenvolvido em Java com Spring Boot.
- **Supabase** — Utilizado para hospedar o banco de dados PostgreSQL.

