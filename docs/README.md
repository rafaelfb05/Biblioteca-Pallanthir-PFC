# Biblioteca Pallanthir

Um sistema de biblioteca criado para ajudar estudantes de diversas áreas.

Os estudantes podem navegar pelo catálogo organizado por matérias, avaliar os livros e conferir os mais bem avaliados, salvar os preferidos na aba de favoritos, reservar livros para retirada, comprar exemplares pagando pelo Mercado Pago e pedir um aviso por e-mail quando um livro esgotado voltar ao estoque. Os funcionários cuidam do acervo, das entregas, devoluções, retiradas e multas e acompanham tudo pela dashboard. O gerente cuida da equipe, dos documentos legais, da dashboard de vendas e dos logs de auditoria.

O sistema não oferece os livros digitalmente: as reservas e as compras são de exemplares físicos da biblioteca.

---

## Funcionalidades

### Estudante
- Catálogo de livros em páginas de 18, com busca por título, filtro por matéria e ranking dos 10 mais bem avaliados, também filtrável por matéria.
- Página de detalhes do livro com capa ampliável, ISBN, preço, estoque para reserva e estoque à venda.
- Avaliação dos livros com nota de 1 a 5 e comentário opcional.
- Favoritos.
- Reserva de livros com aceite da Política de Uso de Reservas, prazos de retirada (3 dias) e de devolução (7 dias), limite de 3 reservas em aberto, cancelamento e histórico em "Minhas reservas".
- Carrinho de compras e pagamento pelo **Mercado Pago (Checkout Pro)**.
- Página "Meus pedidos", com o status de cada compra, cancelamento de pedidos não pagos, prazo de retirada e pedido de reembolso.
- Pagamento de multas por dano pelo Mercado Pago.
- Aviso de disponibilidade: um sino em cada estoque esgotado envia um e-mail, com a capa do livro, quando o estoque volta.

### Funcionário
- Cadastro de livros a partir do título, com dados da Google Books API e capa da Open Library (pelo ISBN ou pelo título e autor), link manual da capa e capa genérica quando nada é encontrado.
- Bloqueio do cadastro de livros que já estão no acervo.
- Edição e exclusão de livros, com estoques separados para reserva e para venda.
- Gestão das reservas, das compras para retirar e das multas, com registro de entrega, devolução (com ou sem dano) e retirada.
- Dashboard com os números do acervo, das reservas e das matérias mais reservadas e mais compradas.

### Gerente
- Todos os acessos do funcionário.
- **Equipe:** promover funcionários a gerente, rebaixar gerentes e excluir contas de funcionários. O sistema nunca fica sem gerente.
- **Documentos:** edição dos Termos de Uso, da Política de Privacidade e da Política de Reservas pelo site, com histórico de versões.
- **Vendas:** dashboard de vendas por período.
- **Logs:** consulta dos logs de auditoria com filtros por usuário, ação e período, busca e páginas de 50 registros. Os e-mails aparecem escondidos, e o gerente pode ver o e-mail completo pelo ícone de olho, com a visualização registrada.

### Conta e segurança
- Cadastro como Estudante ou Funcionário (este último com código de acesso). Gerentes são promovidos por outro gerente.
- Login com **verificação em duas etapas**: um código de 6 dígitos é enviado por e-mail.
- Bloqueio da conta por 15 minutos após 3 tentativas de senha incorreta, e códigos invalidados após 5 tentativas erradas.
- Recuperação de senha por código enviado por e-mail.
- Sessão encerrada automaticamente após 15 minutos sem atividade.
- Página "Minha conta" para editar os dados, trocar o e-mail com confirmação por código, trocar a senha e excluir a conta com anonimização dos dados.
- Termos de Uso e Política de Privacidade (LGPD) exibidos na aplicação, com aceite obrigatório no cadastro e novo aceite sempre que mudarem.
- E-mails parcialmente escondidos em todas as telas, exceto na página da própria conta.

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

- **PostgreSQL** — Banco de dados principal do sistema, hospedado na Supabase. É responsável pela persistência dos usuários, livros, favoritos, reservas, avaliações, carrinhos, pedidos, multas, avisos de disponibilidade e documentos legais com as suas versões.
- **MongoDB** — Usado **exclusivamente para os logs de auditoria**, sem nenhum dado de negócio. Armazena registros como logins, reservas, pedidos, pagamentos, alterações de livros e tentativas de acesso negado, apagados automaticamente após 12 meses.

### Integrações externas

- **Google Books API** — API externa utilizada para obter informações dos livros adicionados ao sistema.
- **Open Library** — Fonte das capas dos livros, buscadas pelo ISBN ou pelo título e autor. Não exige chave.
- **Brevo** — Serviço de envio de e-mails transacionais, utilizado para enviar os códigos de verificação em duas etapas, de recuperação de senha e de troca de e-mail, e os avisos de disponibilidade.
- **Mercado Pago** — Processa os pagamentos das compras e das multas e os reembolsos pela API de Orders (Checkout Pro) e avisa o sistema por webhook quando um pagamento muda de status.

### Hospedagem

- **Vercel** — Utilizada para hospedar e disponibilizar o front-end.
- **Render** — Utilizado para hospedar o back-end desenvolvido em Java com Spring Boot.
- **Supabase** — Utilizado para hospedar o banco de dados PostgreSQL.

---

## Estrutura do projeto

```
Biblioteca-Pallanthir-PFC/
├── Dockerfile          -> build do back-end (usado pelo Render)
├── .github/workflows/  -> integração contínua (GitHub Actions)
├── backend/            -> API em Java 24 + Spring Boot (Maven)
│   ├── src/main/resources/application.properties
│   └── src/main/resources/documentos/ -> versão inicial dos documentos legais
├── docs/               -> README, manuais, Release Notes, Requisitos, Segurança, documentos legais e scripts da migração para UUID
└── frontend/           -> aplicação React (Create React App)
    ├── scripts/copiar-docs.js
    ├── .env.development
    ├── .env.production
    └── vercel.json
```

Os Termos de Uso, a Política de Privacidade e a Política de Reservas ficam guardados no banco de dados e são carregados pela API (`/documentos`). Na primeira inicialização, se o banco ainda não tiver os documentos, eles são criados a partir de `backend/src/main/resources/documentos/`. Depois disso, as alterações são feitas pelo gerente na página **Documentos** do site, e os arquivos em `docs/` servem de cópia de referência.

O script `frontend/scripts/copiar-docs.js` ainda roda antes do `npm start` e do `npm run build` e copia os documentos de `docs/` para `frontend/public/docs/`, mas a aplicação não usa mais esses arquivos.

---

## Pré-requisitos

- **JDK 24** (o `pom.xml` exige `java.version` 24)
- **Node.js 18+** e **npm**
- **Git**
- Conta na **Supabase**, na **Render** e na **Vercel** (para o deploy)
- **MongoDB** para os logs de auditoria (por exemplo, no **MongoDB Atlas**)
- Conta na **Brevo**
- Chave da **Google Books API**
- Conta de desenvolvedor no **Mercado Pago**

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
| `FRONTEND_URL` | Não | URL do front-end, usada no link "Copiar código" dos e-mails, no link do livro dos avisos de disponibilidade e nos endereços de retorno do pagamento. Padrão: `https://pallanthir.vercel.app` |
| `BOOK_API` | Sim | Chave da Google Books API, usada pelo `LivroService` |
| `MP_ACCESS_TOKEN` | Sim | Access Token da aplicação no Mercado Pago, usado para criar e consultar os pedidos |
| `MP_WEBHOOK_SECRET` | Sim | Assinatura secreta do webhook do Mercado Pago, usada para validar as notificações |
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

Depois do primeiro start, ative o **Row Level Security** em todas as tabelas (*Table Editor → RLS*), sem criar políticas. O back-end continua acessando normalmente, e as chaves públicas da Supabase deixam de ler os dados.

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

O banco (`MONGODB_DATABASE`, padrão `pallanthir`) e a coleção `logs_auditoria` são criados automaticamente no primeiro log gravado. Na inicialização, o back-end cria o índice `expiracao_dos_logs`, que apaga cada log 12 meses depois do registro.

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

#### 4.1 Primeiro gerente

Não existe cadastro de Gerente. Crie uma conta de Funcionário pelo site e promova-a no **SQL Editor** da Supabase:

```sql
UPDATE usuarios SET tipo = 'GERENTE' WHERE email = 'email-do-gerente@exemplo.com' AND ativo = true;
```

Os próximos gerentes são promovidos por ele na página **Equipe**.

### 5. Google Books API

1. Acesse o [Google Cloud Console](https://console.cloud.google.com) e crie um projeto.
2. Em **APIs e serviços → Biblioteca**, ative a **Books API**.
3. Em **APIs e serviços → Credenciais**, crie uma **Chave de API**.
4. Essa chave é o valor de `BOOK_API`.

### 6. Mercado Pago (pagamentos)

1. Acesse [mercadopago.com.br/developers](https://www.mercadopago.com.br/developers), entre com a conta da biblioteca e abra **Suas integrações**.
2. Clique em **Criar aplicação**, escolha **Pagamentos online** e o produto **Checkout Pro**.
3. Na aplicação, selecione o ambiente (**Teste** ou **Produção**) e abra **Credenciais**. Copie o **Access Token** (não a Public Key). Ele é o `MP_ACCESS_TOKEN`.
4. No mesmo ambiente, abra **Configuração de webhooks** e preencha:
   - **URL:** `https://<sua-api>/pagamentos/webhook` (a URL da Render)
   - **Eventos:** marque apenas **Order (Mercado Pago)**
5. Salve e copie a **Assinatura secreta**. Ela é o `MP_WEBHOOK_SECRET`.

As credenciais de teste e as de produção são diferentes, assim como o webhook de cada ambiente. Troque as duas variáveis juntas ao mudar de ambiente.

Para testar sem cobrança real, use as credenciais de **Teste** e crie uma conta compradora em **Suas integrações → Contas de teste**. Veja a seção [Pagamentos](#pagamentos).

### 7. Render (back-end)

Chaves são geradas no momento do deploy; veja a seção de deploy abaixo.

### 8. Vercel (front-end)

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
$env:MP_ACCESS_TOKEN = "seu-access-token-do-mercado-pago"
$env:MP_WEBHOOK_SECRET = "sua-assinatura-secreta-do-webhook"

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
export MP_ACCESS_TOKEN="seu-access-token-do-mercado-pago"
export MP_WEBHOOK_SECRET="sua-assinatura-secreta-do-webhook"

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
  -e MP_ACCESS_TOKEN="seu-access-token-do-mercado-pago" \
  -e MP_WEBHOOK_SECRET="sua-assinatura-secreta-do-webhook" \
  pallanthir-backend
```

Rodando localmente, o Mercado Pago não consegue chamar o webhook em `localhost`. O status dos pedidos ainda é atualizado, porque o back-end consulta o Mercado Pago sempre que a página "Meus pedidos" é aberta.

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

## Pagamentos

O fluxo de compra funciona assim:

1. O estudante adiciona livros ao carrinho (`POST /carrinho/itens`).
2. Ao clicar em **Pagar com Mercado Pago**, o back-end cria o pedido (`POST /pedidos/checkout`), separa os exemplares do estoque de venda por **30 minutos** e cria uma order no Mercado Pago, que devolve o link de pagamento.
3. O estudante paga na página do Mercado Pago e volta ao site em `/compra/sucesso`, `/compra/pendente` ou `/compra/falha`.
4. O Mercado Pago chama o webhook (`POST /pagamentos/webhook`). O back-end valida a assinatura, consulta a order na API do Mercado Pago e marca o pedido como **Aprovado** quando o pagamento é processado.
5. Ao abrir "Meus pedidos", o back-end também consulta o Mercado Pago para cada pedido ainda pendente, garantindo o status correto mesmo sem o webhook.
6. Com o pagamento aprovado, o estudante retira os livros na biblioteca em até **15 dias**, e o funcionário registra a retirada na aba **Compras para retirar** da Gestão.

Regras importantes:
- Os itens só saem do carrinho quando o pedido é aprovado ou expira.
- Enquanto houver um pedido aguardando pagamento, o carrinho fica bloqueado e não é possível criar outro pedido. O estudante pode cancelar esse pedido.
- Pedidos não pagos expiram após 30 minutos; os exemplares voltam ao estoque de venda.
- Enquanto os livros não forem retirados, o estudante pode pedir o reembolso pelo site. Compras não retiradas em 15 dias são reembolsadas automaticamente.
- Não é possível comprar com uma multa pendente ou sem ter aceitado a versão atual dos termos.
- As multas por dano são pagas pelo mesmo fluxo do Mercado Pago, e o link de pagamento vale 60 minutos.

### Testando com as contas de teste

1. Use as credenciais de **Teste** em `MP_ACCESS_TOKEN` e `MP_WEBHOOK_SECRET`.
2. Em **Suas integrações → Contas de teste**, use ou crie uma conta **Compradora**.
3. Abra uma janela anônima, entre em [mercadopago.com.br](https://www.mercadopago.com.br) com a conta compradora de teste e, na mesma janela, faça a compra pelo site.
4. Pague com um dos cartões de teste exibidos no painel, usando o titular **`APRO`** para simular a aprovação.

Se aparecer "Uma das partes com as quais você está tentando efetuar o pagamento é de teste", o pagamento está misturando contas de teste e reais: confira se o `MP_ACCESS_TOKEN` é de teste e se o pagamento foi feito logado com a conta compradora de teste.

---

## Deploy

### Back-end na Render

1. Acesse [render.com](https://render.com) e conecte sua conta do GitHub.
2. Clique em **New → Web Service** e selecione o repositório.
3. Configure:
   - **Runtime:** Docker (a Render detecta o `../Dockerfile` na raiz)
   - **Root Directory:** deixe em branco (a raiz do repositório)
   - **Branch:** `main`
   - **Instance Type:** Free já atende (o `../Dockerfile` limita o heap da JVM a 256 MB)
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
   - `MP_ACCESS_TOKEN`
   - `MP_WEBHOOK_SECRET`
   - `CORS_ALLOWED_ORIGINS` com a URL do front na Vercel (opcional, se for diferente do padrão)

   Não cadastre `PORT` — a Render injeta essa variável sozinha.
5. Clique em **Create Web Service**. Ao final, anote a URL gerada, no formato `https://seu-servico.onrender.com`.
6. No painel do Mercado Pago, configure o webhook com essa URL seguida de `/pagamentos/webhook`.

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

Ao atualizar um banco da v2.0 para a v3.0, rode a migração dos estoques descrita nas [Release Notes v3.0](RELEASE_NOTES_V3.0.md#migração-do-banco-de-dados). Ao atualizar da v3.0 para a v4.0, siga a migração da [v4.0](RELEASE_NOTES_v4.0.md): identificadores UUID, primeiro gerente e RLS.

---

## Integração contínua

O workflow `.github/workflows/ci.yml` roda a cada push e a cada pull request para a `main`: compila e testa o back-end com `./mvnw -B verify` e roda os testes e o build do front-end.

---

## Documentação

| Documento | Conteúdo |
|---|---|
| [Requisitos](REQUISITOS.md) | Requisitos funcionais, não funcionais e regras de negócio |
| [Release Notes v1.0](RELEASE_NOTES_v1.0.md) | Primeira versão do sistema |
| [Release Notes v2.0](RELEASE_NOTES_V2.0.md) | Segurança, 2FA, logs e LGPD |
| [Release Notes v3.0](RELEASE_NOTES_V3.0.md) | Avaliações, carrinho, pagamentos com Mercado Pago e dashboard |
| [Release Notes v4.0](RELEASE_NOTES_v4.0.md) | Perfil Gerente, política e prazos das reservas, multas, reembolsos, avisos de disponibilidade, capas pela Open Library e e-mails escondidos |
| [Política de Segurança](SECURITY.md) | Mecanismos de segurança e como relatar vulnerabilidades |
| [Termos de Uso](TERMOS_DE_USO.md) | Regras de uso do sistema |
| [Política de Privacidade](POLITICA_DE_PRIVACIDADE.md) | Tratamento de dados pessoais conforme a LGPD |
| [Política de Reservas](POLITICA_DE_RESERVAS.md) | Prazos, limites, danos e multas das reservas |
| [Política de Reembolso](POLITICA_DE_REEMBOLSO.md) | Cancelamentos, reembolsos, arrependimento e como o dinheiro volta |
| [Manual do Usuário](MANUAL_DO_USUARIO.md) | Passo a passo do estudante: conta, catálogo, reservas, compras, multas e avisos |
| [Manual do Funcionário](MANUAL_DO_FUNCIONARIO.md) | Passo a passo da equipe: acervo, gestão do balcão, multas, dashboard e área do gerente |

---
