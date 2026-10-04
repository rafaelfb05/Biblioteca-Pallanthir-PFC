# Biblioteca Pallanthir

Um sistema de biblioteca criado para ajudar estudantes de diversas áreas.

Os estudantes podem navegar pelo catálogo organizado por matérias, conferir os livros mais bem avaliados, salvar os preferidos na aba de favoritos e reservar livros para retirada. Os funcionários cuidam do acervo, das entregas e devoluções e acompanham tudo pelos logs de auditoria.

O sistema não oferece os livros digitalmente, apenas reservas para buscar em uma biblioteca física.

---

## Funcionalidades

### Estudante
- Catálogo de livros com busca por título, filtro por matéria e página dos mais bem avaliados.
- Página de detalhes do livro com capa ampliável, preço e estoque.
- Favoritos.
- Reserva de livros, cancelamento e histórico em "Minhas reservas".

### Funcionário
- Cadastro de livros a partir do título, com dados e capa preenchidos pela Google Books API.
- Edição e exclusão de livros.
- Gestão de usuários e reservas, com registro de entrega e devolução.
- Consulta dos logs de auditoria, com filtro por usuário e por texto.

### Conta e segurança
- Cadastro como Estudante ou Funcionário (este último com código de acesso).
- Login com **verificação em duas etapas**: um código de 6 dígitos é enviado por e-mail.
- Bloqueio da conta por 15 minutos após 3 tentativas de senha incorreta.
- Recuperação de senha por código enviado por e-mail.
- Sessão encerrada automaticamente após 15 minutos sem atividade.
- Página "Minha conta" para editar os dados, trocar a senha e excluir a conta com anonimização dos dados.
- Termos de Uso e Política de Privacidade (LGPD) exibidos na aplicação, com aceite obrigatório no cadastro.

---

## Tecnologias utilizadas

### Back-end

- **Java** — Linguagem de programação usada no back-end. Oferece recursos adequados para o desenvolvimento de aplicações robustas, escaláveis e orientadas a objetos.
- **Spring Boot** — Framework para desenvolvimento de aplicações Java. Fornece a infraestrutura para criação de aplicações web e APIs, facilitando a organização do projeto e oferecendo diversas dependências que agilizam o desenvolvimento.
- **Spring Security** — Utilizado para implementar os mecanismos de autenticação e autorização da aplicação, controlando o acesso de cada rota conforme o tipo de usuário.
- **JWT (Json Web Token)** — Tecnologia utilizada no processo de autenticação e autorização dos usuários, permitindo a transmissão segura de informações relacionadas à sessão e à identidade do usuário.
- **BCrypt** — Algoritmo utilizado para gerar hashes das senhas dos usuários, permitindo armazená-las de forma segura no banco de dados.
- **Swagger / OpenAPI** — Documentação interativa da API.

### Front-end

- **HTML** — Linguagem de marcação usada no front-end. Funciona como o esqueleto do site.
- **CSS** — Linguagem responsável por definir a aparência visual e o estilo das páginas web escritas em HTML.
- **JavaScript** — Linguagem que adiciona dinamismo, interatividade e inteligência às páginas web, controlando ações e comportamentos dos elementos na tela.
- **React** — Biblioteca JavaScript para front-end baseada em uma arquitetura de componentes reutilizáveis, permitindo dividir a interface em partes independentes.

### Banco de dados

- **PostgreSQL** — Banco de dados principal do sistema, hospedado na Supabase. É responsável pela persistência dos usuários, livros, favoritos e reservas.
- **MongoDB** — Usado **exclusivamente para os logs de auditoria**, sem nenhum dado de negócio. Armazena registros como logins, reservas, alterações de livros e tentativas de acesso negado.

### Integrações externas

- **Google Books API** — API externa utilizada para obter informações dos livros adicionados ao sistema.
- **Brevo** — Serviço de envio de e-mails transacionais, utilizado para enviar os códigos de verificação em duas etapas e de recuperação de senha.

### Hospedagem

- **Vercel** — Utilizada para hospedar e disponibilizar o front-end.
- **Render** — Utilizado para hospedar o back-end desenvolvido em Java com Spring Boot.
- **Supabase** — Utilizado para hospedar o banco de dados PostgreSQL.

---

## Estrutura do projeto

```
Biblioteca-Pallanthir-PFC/
├── Dockerfile          -> build do back-end (usado pelo Render)
├── backend/            -> API em Java 24 + Spring Boot (Maven)
│   └── src/main/resources/application.properties
├── docs/               -> README, Release Notes, Requisitos, Segurança, Termos de Uso e Política de Privacidade
└── frontend/           -> aplicação React (Create React App)
    ├── scripts/copiar-docs.js
    ├── .env.development
    ├── .env.production
    └── vercel.json
```

O script `frontend/scripts/copiar-docs.js` roda automaticamente antes do `npm start` e do `npm run build`, copiando os Termos de Uso e a Política de Privacidade da pasta `docs/` para `frontend/public/docs/`, de onde são exibidos na aplicação.

---

## Pré-requisitos

- **JDK 24** (o `pom.xml` exige `java.version` 24)
- **Node.js 18+** e **npm**
- **Git**
- Conta na **Supabase**, na **Render** e na **Vercel** (para o deploy)
- **MongoDB** para os logs de auditoria (por exemplo, no **MongoDB Atlas**)
- Conta na **Brevo**
- Chave da **Google Books API**

---

## Variáveis de ambiente

### Back-end

| Variável | Obrigatória | Descrição |
|---|---|---|
| `SUPABASE_URL` | Sim | URL JDBC do PostgreSQL da Supabase. Ex.: `jdbc:postgresql://aws-0-sa-east-1.pooler.supabase.com:5432/postgres` |
| `SUPABASE_USER` | Sim | Usuário do banco. Ex.: `postgres.xxxxxxxxxxxx` |
| `SUPABASE_PASSWORD` | Sim | Senha do banco definida na criação do projeto Supabase |
| `MONGODB_URI` | Sim | String de conexão do MongoDB onde ficam os logs de auditoria. Ex.: `mongodb+srv://usuario:senha@cluster0.xxxxx.mongodb.net/` |
| `MONGODB_DATABASE` | Não | Nome do banco de logs no MongoDB. Padrão: `pallanthir` |
| `JWT_SECRET` | Sim | Chave usada para assinar os tokens JWT. Deve ter pelo menos 32 caracteres |
| `CODIGO_FUNCIONARIO` | Sim | Código exigido para criar uma conta do tipo Funcionário |
| `BREVO_API_KEY` | Sim | Chave da API da Brevo, usada para enviar os e-mails |
| `EMAIL_REMETENTE` | Sim | E-mail remetente verificado na Brevo |
| `FRONTEND_URL` | Não | URL do front-end, usada no link "Copiar código" dos e-mails. Padrão: `https://pallanthir.vercel.app` |
| `BOOK_API` | Sim | Chave da Google Books API, usada pelo `LivroService` |
| `PORT` | Não | Porta do servidor. Padrão `8080`. A Render define automaticamente |
| `CORS_ALLOWED_ORIGINS` | Não | Origens liberadas no CORS, separadas por vírgula. Padrão: `http://localhost:3000,https://pallanthir.vercel.app,https://*.vercel.app` |

### Front-end

| Variável | Obrigatória | Descrição |
|---|---|---|
| `REACT_APP_API_URL` | Sim | Endereço da API. Em desenvolvimento `http://localhost:8080`, em produção a URL da Render |

O front-end já traz `.env.development` e `.env.production` versionados. Para sobrescrever localmente sem alterar o repositório, copie `../frontend/.env.example` para `frontend/.env.local`.

---

## Como obter as chaves

### 1. Supabase (banco PostgreSQL)

1. Acesse [supabase.com](https://supabase.com) e crie um projeto.
2. Guarde a **senha do banco** definida nesse momento — ela é `SUPABASE_PASSWORD`.
3. No painel do projeto, vá em **Connect** (ou *Project Settings → Database*).
4. Copie a string de conexão do **Session pooler** ou **Transaction pooler**. Ela tem o formato:
   ```
   postgresql://postgres.abcdefghijklmno:SENHA@aws-0-sa-east-1.pooler.supabase.com:5432/postgres
   ```
5. Converta para o formato JDBC, que é o esperado pelo Spring:
   - `SUPABASE_URL` = `jdbc:postgresql://aws-0-sa-east-1.pooler.supabase.com:5432/postgres`
   - `SUPABASE_USER` = `postgres.abcdefghijklmno`
   - `SUPABASE_PASSWORD` = a senha do passo 2

Não é necessário criar as tabelas manualmente: o Hibernate está com `ddl-auto=update` e gera o esquema no primeiro start.

### 2. MongoDB (somente logs de auditoria)

O MongoDB não guarda usuários, livros nem reservas; esses dados ficam no PostgreSQL da Supabase. Ele recebe apenas os logs de auditoria.

1. Acesse [mongodb.com/atlas](https://www.mongodb.com/atlas) e crie um cluster gratuito (**M0**).
2. Em **Database Access**, crie um usuário com senha e permissão de leitura e escrita.
3. Em **Network Access**, libere o acesso de `0.0.0.0/0`, já que a Render não tem IP fixo no plano gratuito.
4. No cluster, clique em **Connect → Drivers** e copie a string de conexão:
   ```
   mongodb+srv://usuario:senha@cluster0.xxxxx.mongodb.net/
   ```
5. Esse valor, com o usuário e a senha do passo 2, é o `MONGODB_URI`.

O banco (`MONGODB_DATABASE`, padrão `pallanthir`) e a coleção `logs_auditoria` são criados automaticamente no primeiro log gravado.

### 3. Brevo (envio de e-mails)

1. Acesse [brevo.com](https://www.brevo.com) e crie uma conta gratuita.
2. Em **Senders, Domains & Dedicated IPs → Senders**, cadastre o e-mail que vai enviar as mensagens e confirme-o pelo link recebido. Esse e-mail é o `EMAIL_REMETENTE`.
3. Em **SMTP & API → API Keys**, clique em **Generate a new API key** e copie a chave. Ela é o `BREVO_API_KEY`.

O envio é feito pela API HTTP da Brevo (e não por SMTP), que funciona normalmente no plano gratuito da Render.

### 4. JWT_SECRET e CODIGO_FUNCIONARIO

Ambos são definidos por você:

- `JWT_SECRET`: gere um valor aleatório com pelo menos 32 caracteres, por exemplo:
  ```bash
  openssl rand -base64 48
  ```
- `CODIGO_FUNCIONARIO`: escolha um código e repasse apenas aos funcionários da biblioteca. Ele é pedido na tela de cadastro ao escolher o tipo **Funcionário**.

### 5. Google Books API

1. Acesse o [Google Cloud Console](https://console.cloud.google.com) e crie um projeto.
2. Em **APIs e serviços → Biblioteca**, ative a **Books API**.
3. Em **APIs e serviços → Credenciais**, crie uma **Chave de API**.
4. Essa chave é o valor de `BOOK_API`.

### 6. Render (back-end)

Chaves são geradas no momento do deploy; veja a seção de deploy abaixo.

### 7. Vercel (front-end)

Idem — a única variável necessária é `REACT_APP_API_URL`.

---

## Rodando localmente

### 1. Clonar o repositório

```bash
git clone https://github.com/rafaelfb05/Biblioteca-Pallanthir-PFC.git
cd Biblioteca-Pallanthir-PFC
```

### 2. Back-end

Defina as variáveis de ambiente e suba a aplicação.

**Windows (PowerShell):**

```powershell
$env:SUPABASE_URL = "jdbc:postgresql://aws-0-sa-east-1.pooler.supabase.com:5432/postgres"
$env:SUPABASE_USER = "postgres.abcdefghijklmno"
$env:SUPABASE_PASSWORD = "sua-senha"
$env:MONGODB_URI = "mongodb+srv://usuario:senha@cluster0.xxxxx.mongodb.net/"
$env:JWT_SECRET = "uma-chave-aleatoria-com-pelo-menos-32-caracteres"
$env:CODIGO_FUNCIONARIO = "seu-codigo-de-funcionario"
$env:BREVO_API_KEY = "sua-chave-brevo"
$env:EMAIL_REMETENTE = "seu-email-verificado@exemplo.com"
$env:FRONTEND_URL = "http://localhost:3000"
$env:BOOK_API = "sua-chave-google-books"

cd backend
.\mvnw.cmd spring-boot:run
```

**Linux / macOS:**

```bash
export SUPABASE_URL="jdbc:postgresql://aws-0-sa-east-1.pooler.supabase.com:5432/postgres"
export SUPABASE_USER="postgres.abcdefghijklmno"
export SUPABASE_PASSWORD="sua-senha"
export MONGODB_URI="mongodb+srv://usuario:senha@cluster0.xxxxx.mongodb.net/"
export JWT_SECRET="uma-chave-aleatoria-com-pelo-menos-32-caracteres"
export CODIGO_FUNCIONARIO="seu-codigo-de-funcionario"
export BREVO_API_KEY="sua-chave-brevo"
export EMAIL_REMETENTE="seu-email-verificado@exemplo.com"
export FRONTEND_URL="http://localhost:3000"
export BOOK_API="sua-chave-google-books"

cd backend
chmod +x mvnw
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`. A documentação Swagger fica em `http://localhost:8080/swagger-ui.html`.

Para gerar o `.jar` de produção:

```bash
./mvnw clean package -DskipTests
java -jar target/biblioteca-0.0.1-SNAPSHOT.jar
```

### 3. Front-end

Em outro terminal:

```bash
cd frontend
npm install
npm start
```

A aplicação abre em `http://localhost:3000` e já aponta para `http://localhost:8080` por causa do `.env.development`.

### 4. Alternativa: back-end via Docker

```bash
docker build -t pallanthir-backend .
docker run -p 8080:8080 \
  -e SUPABASE_URL="jdbc:postgresql://..." \
  -e SUPABASE_USER="postgres.abcdefghijklmno" \
  -e SUPABASE_PASSWORD="sua-senha" \
  -e MONGODB_URI="mongodb+srv://usuario:senha@cluster0.xxxxx.mongodb.net/" \
  -e JWT_SECRET="uma-chave-aleatoria-com-pelo-menos-32-caracteres" \
  -e CODIGO_FUNCIONARIO="seu-codigo-de-funcionario" \
  -e BREVO_API_KEY="sua-chave-brevo" \
  -e EMAIL_REMETENTE="seu-email-verificado@exemplo.com" \
  -e BOOK_API="sua-chave-google-books" \
  pallanthir-backend
```

---

## Autenticação

O login acontece em duas etapas:

1. `POST /usuarios/login` com e-mail e senha. Se estiverem corretos, um código de 6 dígitos (válido por 10 minutos) é enviado ao e-mail do usuário.
2. `POST /usuarios/verificar-2fa` com e-mail e código. A resposta traz os dados do usuário e um **token JWT** válido por 15 minutos.

As rotas protegidas exigem o cabeçalho:

```
Authorization: Bearer <token>
```

O front-end renova o token automaticamente pela rota `POST /usuarios/renovar-token` enquanto o usuário estiver ativo.

Para testar pelo Swagger, faça as duas etapas acima, copie o token e informe-o nas requisições protegidas.

---

## Deploy

### Back-end na Render

1. Acesse [render.com](https://render.com) e conecte sua conta do GitHub.
2. Clique em **New → Web Service** e selecione o repositório.
3. Configure:
   - **Runtime:** Docker (a Render detecta o `../Dockerfile` na raiz)
   - **Root Directory:** deixe em branco (a raiz do repositório)
   - **Branch:** `main`
   - **Instance Type:** Free já atende (o `../Dockerfile` limita a JVM a 400 MB)
4. Em **Environment → Environment Variables**, cadastre:
   - `SUPABASE_URL`
   - `SUPABASE_USER`
   - `SUPABASE_PASSWORD`
   - `MONGODB_URI`
   - `MONGODB_DATABASE` (opcional, se for diferente de `pallanthir`)
   - `JWT_SECRET`
   - `CODIGO_FUNCIONARIO`
   - `BREVO_API_KEY`
   - `EMAIL_REMETENTE`
   - `FRONTEND_URL` com a URL do front na Vercel (opcional, se for diferente do padrão)
   - `BOOK_API`
   - `CORS_ALLOWED_ORIGINS` com a URL do front na Vercel (opcional, se for diferente do padrão)

   Não cadastre `PORT` — a Render injeta essa variável sozinha.
5. Clique em **Create Web Service**. Ao final, anote a URL gerada, no formato `https://seu-servico.onrender.com`.

No plano gratuito o serviço hiberna após alguns minutos sem uso, então a primeira requisição depois de um período ocioso pode demorar cerca de um minuto.

### Front-end na Vercel

1. Acesse [vercel.com](https://vercel.com) e importe o repositório do GitHub.
2. Configure:
   - **Framework Preset:** Create React App
   - **Root Directory:** `frontend`
   - **Build Command:** `npm run build`
   - **Output Directory:** `build`
3. Em **Settings → Environment Variables**, cadastre para o ambiente *Production*:
   - `REACT_APP_API_URL` = a URL da Render anotada no passo anterior
4. Clique em **Deploy**.

O `vercel.json` já contém o rewrite que envia todas as rotas para o `index.html`, necessário para a navegação do React não quebrar ao recarregar a página.

### Banco na Supabase e logs no MongoDB

Não há passo extra de deploy: o PostgreSQL já está hospedado na Supabase desde a criação do projeto, e o MongoDB dos logs também. Basta garantir que as credenciais cadastradas na Render estejam corretas e que o MongoDB aceite conexões externas (**Network Access**).

---

## Documentação

| Documento | Conteúdo |
|---|---|
| [Requisitos](REQUISITOS.md) | Requisitos funcionais, não funcionais e regras de negócio |
| [Release Notes v1.0](RELEASE_NOTES_v1.0.md) | Primeira versão do sistema |
| [Release Notes v2.0](RELEASE_NOTES_V2.0.md) | Segurança, 2FA, logs e LGPD |
| [Política de Segurança](SECURITY.md) | Mecanismos de segurança e como relatar vulnerabilidades |
| [Termos de Uso](TERMOS_DE_USO.md) | Regras de uso do sistema |
| [Política de Privacidade](POLITICA_DE_PRIVACIDADE.md) | Tratamento de dados pessoais conforme a LGPD |

---

## Checklist final

- [ ] Projeto criado na Supabase e credenciais JDBC anotadas
- [ ] Cluster MongoDB dos logs criado, usuário cadastrado e acesso externo liberado
- [ ] Conta na Brevo com remetente verificado e chave de API gerada
- [ ] `JWT_SECRET` e `CODIGO_FUNCIONARIO` definidos
- [ ] Chave da Google Books API gerada
- [ ] Web Service na Render com todas as variáveis obrigatórias cadastradas e build concluído
- [ ] `REACT_APP_API_URL` na Vercel apontando para a URL da Render
- [ ] URL da Vercel liberada em `CORS_ALLOWED_ORIGINS` e em `FRONTEND_URL`, se for diferente do padrão
