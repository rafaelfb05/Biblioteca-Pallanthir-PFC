# Biblioteca Pallanthir — v1.0

**Data de lançamento:** 14/09/2026

Primeira versão estável da Biblioteca Pallanthir, um sistema de biblioteca voltado para estudantes de diversas áreas. Esta versão reúne o catálogo de livros integrado à Google Books API, cadastro e login de usuários, favoritos, controle de estoque e reserva de livros para retirada em biblioteca física.

---

## Novidades

### Catálogo de livros
- Cadastro de livros a partir do título, com preenchimento automático de autores, ano de lançamento, número de páginas e capa via **Google Books API**.
- Definição de matéria, preço e quantidade em estoque no momento do cadastro.
- Atualização parcial de livros (somente os campos enviados são alterados) e exclusão.
- Busca por título e filtro por matéria.
- Matérias disponíveis: TI, Direito, Medicina, Odontologia, Veterinária, Física, Química, Arquitetura e Biologia.
- Livros sem autor informado recebem "Autor desconhecido".

### Usuários
- Cadastro, listagem, atualização e exclusão de usuários.
- Login por e-mail e senha, com sessão mantida no navegador.

### Favoritos
- Adicionar e remover livros dos favoritos.
- Aba dedicada para listar os livros favoritados.

### Estoque e reservas
- Cada livro possui controle de estoque.
- Reserva de livros com bloqueio no banco (`buscarComLock`), evitando que duas reservas simultâneas consumam a mesma unidade.
- Livros sem estoque não podem ser reservados ("Livro esgotado no momento!").
- Cancelamento e devolução de reservas, devolvendo a unidade ao estoque.
- Status de reserva: `RESERVADO`, `CANCELADO` e `DEVOLVIDO`.
- Aba "Minhas reservas" com o histórico do usuário.

### Front-end (React)
- Página inicial com o catálogo, navegação por matérias, favoritos e reservas.
- Modal de login e cadastro.
- Página de detalhes do livro com preço, estoque e ações de favoritar e reservar.
- Visualizador de capa com zoom (roda do mouse, teclado e arraste) e carregamento da capa em alta resolução.

### Infraestrutura e documentação
- Back-end em **Java 24 + Spring Boot 4.1**, com **PostgreSQL** hospedado na **Supabase**.
- `../Dockerfile` na raiz para deploy na **Render**, com a JVM limitada para caber no plano gratuito.
- Front-end configurado para deploy na **Vercel**, com rewrite de rotas no `vercel.json`.
- CORS configurável pela variável `CORS_ALLOWED_ORIGINS`.
- Documentação da API com **Swagger** em `/swagger-ui.html`.
- README com o passo a passo completo: variáveis de ambiente, obtenção das chaves, execução local e deploy.

---

## Correções
- Corrigido o atributo `preço`, que retornava nulo para livros antigos.
- Corrigida a exclusão de livros.
- Corrigido o formato das capas dos livros.
- Corrigido o deploy na Render (remoção da leitura de entrada no `BibliotecaApplication`, ajustes de permissão do `mvnw` e da versão do Java).

---

## Endpoints da API

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/livros/cadastro` | Cadastra um livro |
| `GET` | `/livros` | Lista todos os livros |
| `GET` | `/livros/titulo/{titulo}` | Busca livros por título |
| `GET` | `/livros/materias` | Lista as matérias |
| `GET` | `/livros/{materia}` | Lista livros de uma matéria |
| `PUT` | `/livros/atualizar/{id}` | Atualiza um livro |
| `DELETE` | `/livros/deletar/{id}` | Exclui um livro |
| `POST` | `/usuarios/cadastro` | Cadastra um usuário |
| `POST` | `/usuarios/login` | Faz login |
| `GET` | `/usuarios` | Lista os usuários |
| `PUT` | `/usuarios/atualizar/{id}` | Atualiza um usuário |
| `DELETE` | `/usuarios/deletar/{id}` | Exclui um usuário |
| `POST` | `/usuarios/{usuarioId}/favoritar/{livroId}` | Adiciona aos favoritos |
| `DELETE` | `/usuarios/{usuarioId}/favoritar/{livroId}` | Remove dos favoritos |
| `GET` | `/usuarios/{usuarioId}/favoritos` | Lista os favoritos |
| `POST` | `/reservas/{usuarioId}/{livroId}` | Reserva um livro |
| `PUT` | `/reservas/{reservaId}/cancelar` | Cancela uma reserva |
| `PUT` | `/reservas/{reservaId}/devolver` | Registra a devolução |
| `GET` | `/reservas/usuario/{usuarioId}` | Lista as reservas do usuário |

---

## Limitações conhecidas
- A autenticação com **Spring Security + JWT** e o hash de senhas com **BCrypt** ainda não foram implementados; as senhas são armazenadas e comparadas em texto puro.
- A integração com o **Mercado Pago** para compras ainda não foi implementada.
- A avaliação dos livros existe no modelo, mas só pode ser alterada pela rota de atualização do livro; ainda não há avaliação feita pelos usuários nem recomendação baseada nela.
- No plano gratuito da Render, a primeira requisição após um período ocioso pode levar cerca de um minuto.

---

## Como executar
Consulte o [README](README.md) para os pré-requisitos, as variáveis de ambiente e o passo a passo de execução local e deploy.
