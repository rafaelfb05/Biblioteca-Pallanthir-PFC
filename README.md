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


---

## Estrutura do projeto

```
Biblioteca-Pallanthir-PFC/
├── Dockerfile          -> build do back-end (usado pelo Render)
├── backend/            -> API em Java 24 + Spring Boot (Maven)
│   └── src/main/resources/application.properties
└── frontend/           -> aplicação React (Create React App)
    ├── .env.development
    ├── .env.production
    └── vercel.json
```

---

## Pré-requisitos

- **JDK 24** (o `pom.xml` exige `java.version` 24)
- **Node.js 18+** e **npm**
- **Git**
- Conta na **Supabase**, na **Render** e na **Vercel** (para o deploy)
- Chave da **Google Books API**

---

## Variáveis de ambiente

### Back-end

| Variável | Obrigatória | Descrição |
|---|---|---|
| `SUPABASE_URL` | Sim | URL JDBC do PostgreSQL da Supabase. Ex.: `jdbc:postgresql://aws-0-sa-east-1.pooler.supabase.com:5432/postgres` |
| `SUPABASE_USER` | Sim | Usuário do banco. Ex.: `postgres.xxxxxxxxxxxx` |
| `SUPABASE_PASSWORD` | Sim | Senha do banco definida na criação do projeto Supabase |
| `BOOK_API` | Sim | Chave da Google Books API, usada pelo `LivroService` |
| `PORT` | Não | Porta do servidor. Padrão `8080`. A Render define automaticamente |
| `CORS_ALLOWED_ORIGINS` | Não | Origens liberadas no CORS, separadas por vírgula. Padrão: `http://localhost:3000,https://pallanthir.vercel.app,https://*.vercel.app` |

### Front-end

| Variável | Obrigatória | Descrição |
|---|---|---|
| `REACT_APP_API_URL` | Sim | Endereço da API. Em desenvolvimento `http://localhost:8080`, em produção a URL da Render |

O front-end já traz `.env.development` e `.env.production` versionados. Para sobrescrever localmente sem alterar o repositório, copie `frontend/.env.example` para `frontend/.env.local`.

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

### 2. Google Books API

1. Acesse o [Google Cloud Console](https://console.cloud.google.com) e crie um projeto.
2. Em **APIs e serviços → Biblioteca**, ative a **Books API**.
3. Em **APIs e serviços → Credenciais**, crie uma **Chave de API**.
4. Essa chave é o valor de `BOOK_API`.

### 3. Render (back-end)

Chaves são geradas no momento do deploy; veja a seção de deploy abaixo.

### 4. Vercel (front-end)

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
$env:BOOK_API = "sua-chave-google-books"

cd backend
.\mvnw.cmd spring-boot:run
```

**Linux / macOS:**

```bash
export SUPABASE_URL="jdbc:postgresql://aws-0-sa-east-1.pooler.supabase.com:5432/postgres"
export SUPABASE_USER="postgres.abcdefghijklmno"
export SUPABASE_PASSWORD="sua-senha"
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
  -e BOOK_API="sua-chave-google-books" \
  pallanthir-backend
```

---

## Deploy

### Back-end na Render

1. Acesse [render.com](https://render.com) e conecte sua conta do GitHub.
2. Clique em **New → Web Service** e selecione o repositório.
3. Configure:
   - **Runtime:** Docker (a Render detecta o `Dockerfile` na raiz)
   - **Root Directory:** deixe em branco (a raiz do repositório)
   - **Branch:** `main`
   - **Instance Type:** Free já atende (o `Dockerfile` limita a JVM a 400 MB)
4. Em **Environment → Environment Variables**, cadastre:
   - `SUPABASE_URL`
   - `SUPABASE_USER`
   - `SUPABASE_PASSWORD`
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

### Banco na Supabase

Não há passo extra de deploy: o banco já está hospedado desde a criação do projeto. Basta garantir que as credenciais cadastradas na Render estejam corretas.

---

## Checklist final

- [ ] Projeto criado na Supabase e credenciais JDBC anotadas
- [ ] Chave da Google Books API gerada
- [ ] Web Service na Render com as quatro variáveis cadastradas e build concluído
- [ ] `REACT_APP_API_URL` na Vercel apontando para a URL da Render
- [ ] URL da Vercel liberada em `CORS_ALLOWED_ORIGINS`, se for diferente do padrão
