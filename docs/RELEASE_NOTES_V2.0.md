# Biblioteca Pallanthir — v2.0

**Data de lançamento:** 28/09/2026

A versão 2.0 é focada em **segurança, privacidade e auditoria**. As senhas passam a ser protegidas com **BCrypt**, o acesso à API é controlado por **Spring Security + JWT**, o login ganha **verificação em duas etapas por e-mail**, as ações do sistema passam a ser registradas em **logs de auditoria no MongoDB Atlas** e o sistema se adequa à **LGPD**, com Termos de Uso, Política de Privacidade e anonimização de dados na exclusão de conta.

---

## Novidades

### Senhas com BCrypt
- As senhas deixam de ser armazenadas em texto puro e passam a ser guardadas **somente como hash BCrypt** (com *salt* aleatório por senha).
- A comparação no login é feita com `PasswordEncoder.matches`, sem nunca recuperar a senha original.
- Nova política de senha forte: **mínimo de 8 caracteres**, com letra maiúscula, minúscula, número e um caractere especial (`@ # $ % ^ & + = !`). A regra é validada no back-end (cadastro) e no front-end.

### Autenticação com Spring Security + JWT
- A API passa a ser protegida pelo **Spring Security**, em modo *stateless* (sem sessão no servidor).
- Após o login, o usuário recebe um **token JWT** assinado com HMAC-SHA, contendo e-mail, id e tipo de conta, com **validade de 15 minutos**.
- O token é enviado no cabeçalho `Authorization: Bearer <token>` e validado pelo novo `JwtAuthFilter` a cada requisição.
- Nova rota `POST /usuarios/renovar-token`: o front-end renova o token automaticamente quando faltam menos de 5 minutos para expirar, desde que o usuário esteja ativo.
- O front-end detecta tokens expirados e encerra a sessão com o aviso "Sua sessão expirou. Entre novamente.".

### Verificação em duas etapas (2FA)
- O login passa a ter duas etapas:
  1. `POST /usuarios/login` confere e-mail e senha e envia um **código de 6 dígitos** por e-mail.
  2. `POST /usuarios/verificar-2fa` confere o código e só então devolve o token JWT.
- O código vale por **10 minutos** e é apagado após o uso.
- Tela dedicada de verificação no front-end, com opção de **reenviar o código**.
- O e-mail traz um botão **"Copiar código"**, que abre uma página da aplicação para copiar o código com um clique.

### Proteção contra força bruta
- Após **3 tentativas** de senha incorreta, a conta fica **bloqueada por 15 minutos**.
- O contador é zerado após um login bem-sucedido.
- A mensagem de erro é genérica ("Email ou senha incorretos"), sem revelar qual dos dois campos está errado.

### Recuperação de senha
- `POST /usuarios/recuperar-senha` envia um **código de 6 dígitos** por e-mail, válido por **15 minutos**.
- `POST /usuarios/redefinir-senha` troca a senha mediante o código, que é apagado após o uso.
- Fluxo completo no front-end ("Esqueci minha senha") e também na página da conta.

### E-mails transacionais com a Brevo
- Novo `EmailService` para envio dos códigos de 2FA e de recuperação de senha.
- O envio por SMTP foi substituído pela **API HTTP da Brevo**, que funciona no plano gratuito da Render.
- Template HTML próprio com a identidade visual da Pallanthir e versão em texto puro.
- Tempo limite de 5 s para conexão e 10 s para leitura, evitando que uma falha no provedor trave a requisição.

### Perfis e permissões
- Dois perfis de usuário: **Estudante** e **Funcionário**.
- O cadastro como Funcionário exige um **código de acesso** (`CODIGO_FUNCIONARIO`), que não é armazenado.
- Permissões por rota:

| Ação | Visitante | Estudante | Funcionário |
|---|:---:|:---:|:---:|
| Ver catálogo, buscar e filtrar livros | ✅ | ✅ | ✅ |
| Cadastrar, editar e excluir livros | | | ✅ |
| Favoritar livros | | ✅ | |
| Reservar e cancelar reservas | | ✅ | |
| Registrar entrega e devolução | | | ✅ |
| Ver as próprias reservas | | ✅ | ✅ |
| Ver reservas de outros usuários | | | ✅ |
| Listar usuários | | | ✅ |
| Consultar logs de auditoria | | | ✅ |
| Editar e excluir a própria conta | | ✅ | ✅ |

- Verificação de **propriedade**: um usuário só pode alterar a própria conta, os próprios favoritos e as próprias reservas. Tentativas contrárias são bloqueadas e registradas como `ACESSO_NEGADO`.

### Logs de auditoria (MongoDB)
- **MongoDB** dedicado aos logs, na coleção `logs_auditoria`.
- Cada log registra usuário, e-mail, ação, descrição e data/hora.
- A gravação é **assíncrona**: se o MongoDB falhar, a operação principal continua e o erro vai para o log da aplicação.
- Ações registradas, entre outras:
  - `CADASTRO_USUARIO`, `FALHA_CADASTRO`, `ATUALIZACAO_USUARIO`, `EXCLUSAO_USUARIO`, `FALHA_EXCLUSAO_USUARIO`
  - `SUCESSO_LOGIN`, `FALHA_LOGIN`, `LOGIN_BLOQUEADO`, `SUCESSO_2FA`, `FALHA_2FA`
  - `SOLICITACAO_RECUPERACAO_SENHA`, `REDEFINICAO_SENHA`, `FALHA_RECUPERACAO_SENHA`, `FALHA_REDEFINICAO_SENHA`
  - `CADASTRO_LIVRO`, `ATUALIZACAO_LIVRO`, `EXCLUSAO_LIVRO`, `FALHA_CADASTRO_LIVRO`
  - `FAVORITAR_LIVRO`, `DESFAVORITAR_LIVRO`
  - `RESERVA_LIVRO`, `FALHA_RESERVA`, `CANCELAMENTO_RESERVA`, `ENTREGA_LIVRO`, `DEVOLUCAO_LIVRO`
  - `ACESSO_NEGADO`
- Nova tela de **Logs** para funcionários, com filtro por usuário e por texto e destaque visual para falhas, acessos negados e bloqueios.

### Logout por inatividade
- A sessão é encerrada automaticamente após **15 minutos sem atividade** (mouse, teclado, rolagem ou toque).
- A última atividade é compartilhada entre abas do navegador.
- Ao reabrir o site, sessões expiradas ou inativas são descartadas.
- O usuário é avisado com a mensagem "Você foi desconectado após 15 minutos sem atividade.".

### LGPD
- Novos documentos **[Termos de Uso](TERMOS_DE_USO.md)** e **[Política de Privacidade](POLITICA_DE_PRIVACIDADE.md)**, exibidos dentro da aplicação em um modal.
- O cadastro exige o aceite dos documentos, e a **data e hora do aceite** são registradas.
- **Exclusão de conta com anonimização**: nome, e-mail, senha, códigos e favoritos são removidos ou substituídos, preservando apenas o histórico de reservas necessário para o controle do acervo.
- A exclusão é bloqueada enquanto houver reservas em aberto ou livros não devolvidos.

### Reservas
- Novo status **`ENTREGUE`**, com o fluxo `RESERVADO` → `ENTREGUE` → `DEVOLVIDO` (ou `RESERVADO` → `CANCELADO`).
- Nova rota `PUT /reservas/{reservaId}/entregar`, exclusiva para funcionários.
- O cancelamento só é permitido antes da entrega, e a devolução só depois dela.
- A restrição de status no banco é atualizada automaticamente na inicialização.

### Livros
- A exclusão de livros passa a ser **lógica** (campo `ativo`), mantendo o histórico das reservas.
- Livros com reservas em aberto exigem confirmação (`?confirmado=true`) para serem excluídos.
- Ao excluir um livro, ele é removido dos favoritos de todos os usuários.
- Nova página de **livros mais bem avaliados** e novo formato de exibição na página principal.

### Front-end
- Nova tela de login com abas de **Entrar** e **Criar conta**, verificação 2FA, recuperação e redefinição de senha.
- Nova página **Minha conta**, para editar dados, trocar a senha e excluir a conta.
- Nova página de **Gestão** para funcionários, com a lista de usuários e o controle de reservas.
- Nova página de **Logs** para funcionários.
- Mensagens de erro da API traduzidas e tratadas, incluindo "Você não tem permissão para fazer isso." para respostas 403.

---

## Correções
- Corrigido o fuso horário: a aplicação passa a usar `America/Sao_Paulo`, evitando horários errados em códigos, reservas e logs.
- Corrigida a exclusão de conta.
- Corrigido o Spring Security bloqueando requisições do front-end (pré-requisições `OPTIONS` do CORS).
- Corrigida a variável de conexão do MongoDB no `application.properties`.
- Corrigido o caminho dos logs.
- Envio de e-mail por SMTP substituído pela Brevo, que não é bloqueada pela Render.

---

## Novas variáveis de ambiente

### Back-end

| Variável | Obrigatória | Descrição |
|---|---|---|
| `JWT_SECRET` | Sim | Chave usada para assinar os tokens JWT. Deve ter **pelo menos 32 caracteres** (256 bits). |
| `CODIGO_FUNCIONARIO` | Sim | Código exigido no cadastro de contas de Funcionário. |
| `MONGODB_URI` | Sim | String de conexão do MongoDB onde ficam os logs de auditoria. |
| `MONGODB_DATABASE` | Não | Nome do banco de logs. Padrão: `pallanthir`. |
| `BREVO_API_KEY` | Sim | Chave da API da Brevo para envio de e-mails. |
| `EMAIL_REMETENTE` | Sim | E-mail remetente verificado na Brevo. |
| `FRONTEND_URL` | Não | URL do front-end usada no link "Copiar código" dos e-mails. Padrão: `https://pallanthir.vercel.app`. |

As variáveis da v1.0 (`SUPABASE_URL`, `SUPABASE_USER`, `SUPABASE_PASSWORD`, `BOOK_API`, `PORT` e `CORS_ALLOWED_ORIGINS`) continuam valendo.

---

## Endpoints da API

### Novos

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| `POST` | `/usuarios/verificar-2fa` | Público | Confirma o código 2FA e devolve o token |
| `POST` | `/usuarios/renovar-token` | Autenticado | Gera um novo token para o usuário logado |
| `POST` | `/usuarios/recuperar-senha` | Público | Envia o código de recuperação |
| `POST` | `/usuarios/redefinir-senha` | Público | Redefine a senha com o código |
| `PUT` | `/reservas/{reservaId}/entregar` | Funcionário | Registra a entrega do livro |
| `GET` | `/logs` | Funcionário | Lista todos os logs |
| `GET` | `/logs/usuarios/{usuarioId}` | Funcionário | Lista os logs de um usuário |

### Alterados

| Método | Rota | Mudança |
|---|---|---|
| `POST` | `/usuarios/cadastro` | Novos campos `codigoAcesso` e `aceitouTermos`; validação de senha forte. |
| `POST` | `/usuarios/login` | Não devolve mais o usuário: apenas envia o código 2FA por e-mail. |
| `DELETE` | `/usuarios/deletar/{id}` | Anonimiza os dados em vez de apagar o registro. |
| `DELETE` | `/livros/deletar/{id}` | Exclusão lógica; novo parâmetro `confirmado`. |
| `PUT` | `/reservas/{reservaId}/devolver` | Exige status `ENTREGUE` e passa a ser exclusiva de funcionários. |

Todas as rotas, exceto as públicas, exigem o cabeçalho `Authorization: Bearer <token>`.

---

## Como executar
Consulte o [README](README.md) para os pré-requisitos e o passo a passo de execução local e deploy, e esta página para as novas variáveis de ambiente.
