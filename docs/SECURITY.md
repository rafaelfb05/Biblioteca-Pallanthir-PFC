# Política de Segurança — Biblioteca Pallanthir

**Última atualização:** 22/11/2026

Este documento descreve as versões que recebem correções de segurança, como relatar uma vulnerabilidade e quais mecanismos de proteção a **Biblioteca Pallanthir** utiliza.

Para saber como tratamos os seus dados pessoais, consulte a [Política de Privacidade](POLITICA_DE_PRIVACIDADE.md) e os [Termos de Uso](TERMOS_DE_USO.md).

---

## 1. Versões suportadas

| Versão | Suportada | Observação |
|---|:---:|---|
| 4.0 | ✅ | Versão atual, com perfil Gerente, códigos mais seguros, conferência da conta a cada requisição e e-mails escondidos. |
| 3.0 | ❌ | Substituída pela 4.0. As correções de segurança são publicadas apenas na versão atual. |
| 2.0 | ❌ | Substituída pela 3.0. |
| 1.0 | ❌ | Armazenava senhas em texto puro e não tinha autenticação na API. Não deve ser usada. |

---

## 2. Como relatar uma vulnerabilidade

Se você encontrou uma falha de segurança, **não abra uma issue pública** no GitHub, para que ela não seja explorada antes da correção.

Envie um e-mail para **[bibliotecapallanthir@gmail.com](mailto:bibliotecapallanthir@gmail.com)** com o assunto `[SEGURANÇA]` contendo:

- descrição da vulnerabilidade e o impacto esperado;
- passos para reproduzir (rota, requisição, dados usados);
- versão ou commit afetado;
- sugestão de correção, se houver.

### O que esperar
| Etapa | Prazo |
|---|---|
| Confirmação de recebimento | até 3 dias úteis |
| Avaliação inicial e classificação de gravidade | até 7 dias úteis |
| Correção de falhas críticas ou altas | o quanto antes, priorizada sobre novas funcionalidades |

Você será informado sobre o andamento e poderá ser creditado na release que trouxer a correção, se desejar.

### Escopo
- **Dentro do escopo:** o back-end deste repositório, o front-end hospedado na Vercel e os fluxos de cadastro, login, 2FA, recuperação de senha, troca de e-mail, reservas, multas, avaliações, carrinho, pedidos, reembolsos, avisos de disponibilidade, webhook de pagamento, dashboards, documentos, gestão da equipe e logs.
- **Fora do escopo:** serviços de terceiros (Supabase, MongoDB, Render, Vercel, Brevo, Google Books, Open Library, Mercado Pago), ataques de negação de serviço, engenharia social, testes que acessem ou alterem dados de outros usuários reais e qualquer teste que gere cobranças reais. Para testar pagamentos, use as credenciais e as contas de teste do Mercado Pago.

---

## 3. Mecanismos de segurança

### 3.1 Senhas
- Armazenadas **somente como hash BCrypt**, com *salt* aleatório por senha (`PasswordEncoderConfig`).
- A senha original nunca é gravada, registrada em log ou devolvida pela API.
- Política de senha forte: mínimo de 8 caracteres, com letra maiúscula, minúscula, número e caractere especial (`@ # $ % ^ & + = !`).

### 3.2 Autenticação
- **Verificação em duas etapas (2FA):** após a senha correta, um código de 6 dígitos é enviado por e-mail. O código vale **10 minutos** e é apagado após o uso.
- **Códigos seguros:** os códigos de 2FA, de recuperação de senha e de troca de e-mail são gerados com `SecureRandom`, comparados em **tempo constante** e **invalidados após 5 tentativas erradas**.
- **JWT:** após o 2FA, a API emite um token assinado com HMAC-SHA (`JWT_SECRET`), com **validade de 15 minutos**, enviado no cabeçalho `Authorization: Bearer <token>`.
- **API stateless:** não há sessão no servidor (`SessionCreationPolicy.STATELESS`).
- **Conta conferida a cada requisição:** o `JwtAuthFilter` carrega o usuário do banco e só aceita o token se a conta estiver ativa, com o mesmo e-mail e o perfil atual. Excluir a conta, trocar o e-mail ou mudar o perfil invalida os tokens antigos na hora.
- **Renovação automática:** o front-end renova o token quando faltam menos de 5 minutos para expirar, apenas se o usuário estiver ativo.
- **Logout por inatividade:** a sessão é encerrada após **15 minutos sem atividade**.

### 3.3 Proteção contra força bruta
- Após **3 tentativas** de senha incorreta, a conta é **bloqueada por 15 minutos**.
- A mensagem de erro do login é genérica ("Email ou senha incorretos").

### 3.4 Recuperação de senha e troca de e-mail
- Código de 6 dígitos enviado por e-mail, válido por **15 minutos** e apagado após o uso.
- Um novo código só pode ser pedido depois de **1 minuto**.
- As respostas são genéricas ("Se o e-mail estiver cadastrado…", "Código inválido ou expirado"), sem revelar se a conta existe.
- **Troca de e-mail:** o novo e-mail só é gravado depois da confirmação de um código enviado ao **novo endereço**, válido por 15 minutos. Depois da troca, é preciso entrar de novo.

### 3.5 Autorização
- Controle de acesso por perfil no **Spring Security** (`SecurityConfig`):
  - **Público:** catálogo de livros, avaliações, ranking e documentos legais (somente leitura), cadastro, login, 2FA e recuperação de senha.
  - **Estudante:** favoritos, reservas e cancelamentos, avaliações, carrinho, pedidos, cancelamentos e reembolsos, multas e avisos de disponibilidade.
  - **Funcionário:** cadastro, edição e exclusão de livros, entrega e devolução, retirada de compras, multas, lista de usuários e dashboard.
  - **Gerente:** tudo o que o funcionário faz, além de logs, dashboard de vendas, edição dos documentos e gestão da equipe. O perfil `GERENTE` recebe as permissões `ROLE_GERENTE` e `ROLE_FUNCIONARIO`.
  - **Webhook de pagamento:** a rota `POST /pagamentos/webhook` não exige login, mas só processa notificações com assinatura válida do Mercado Pago (seção 3.8).
- **Verificação de propriedade:** além do perfil, o serviço confere se o recurso pertence ao usuário logado (conta, favoritos, reservas, avaliações, carrinho, pedidos, multas e avisos). O carrinho e os pedidos são sempre buscados a partir do usuário do token, nunca de um identificador enviado na requisição. Tentativas contrárias são negadas e, nos fluxos de conta, favoritos e reservas, registradas como `ACESSO_NEGADO`.
- O cadastro de Funcionário exige o código `CODIGO_FUNCIONARIO`, que não é armazenado.
- **Não existe cadastro de Gerente:** o perfil só é dado por outro gerente. O último gerente ativo não pode ser rebaixado nem excluído, e essa checagem usa **bloqueio pessimista** para resistir a pedidos simultâneos.
- Reservas e compras exigem o aceite da **versão atual** dos Termos de Uso e da Política de Privacidade, e as reservas também da Política de Reservas.

### 3.6 Auditoria
- Ações sensíveis são registradas no **MongoDB** (coleção `logs_auditoria`): cadastros, logins, falhas de login, bloqueios, 2FA, recuperação de senha, trocas de e-mail, alterações em livros, reservas, multas, avaliações, pedidos, pagamentos, reembolsos, avisos, alterações de documentos e da equipe e acessos negados.
- Pagamentos aprovados em pedidos que já tinham expirado são registrados como `PAGAMENTO_EM_PEDIDO_EXPIRADO`, para conferência manual.
- Os logs **não contêm senhas, hashes nem códigos** de verificação.
- Somente **gerentes** podem consultar os logs.
- Os e-mails chegam à tela dos logs **parcialmente escondidos**, inclusive dentro da descrição. O gerente pode ver o e-mail completo de um registro, e cada visualização é registrada como `VISUALIZOU_EMAIL`, sem o e-mail no texto.
- A busca por e-mail nos logs compara um resumo do e-mail completo digitado, sem enviar os e-mails ao navegador e sem permitir busca por pedaços, que deixaria descobrir um e-mail letra por letra.
- Os logs são **apagados automaticamente após 12 meses** por um índice TTL (`expiracao_dos_logs`).

### 3.7 Privacidade (LGPD)
- Aceite dos Termos de Uso e da Política de Privacidade registrado com data, hora e **versão**. Quando os documentos mudam, um novo aceite é pedido.
- Exclusão de conta com **anonimização**: nome, e-mail, senha, códigos e favoritos são removidos ou substituídos, e os avisos de disponibilidade são cancelados.
- **E-mails escondidos:** fora da página da própria conta, as respostas da API trazem os e-mails parcialmente escondidos (`fe•••@gmail.com`), na Gestão, na Equipe e nos Logs.

### 3.8 Pagamentos
- **Sem dados de cartão no sistema:** o pagamento é feito na página do Mercado Pago (Checkout Pro). A Pallanthir não recebe, não processa e não armazena número de cartão, código de segurança ou dados bancários.
- **Preço definido pelo servidor:** o valor de cada item é lido do banco no momento do pedido. O front-end não envia preços.
- **Webhook assinado:** toda notificação recebida em `POST /pagamentos/webhook` tem o cabeçalho `x-signature` validado com **HMAC-SHA256** e a assinatura secreta (`MP_WEBHOOK_SECRET`), usando o `x-request-id` e o ID da notificação. A comparação é feita em tempo constante. Notificações inválidas recebem `401`.
- **Status confirmado na fonte:** o conteúdo da notificação não é usado para mudar o status. O back-end consulta a order na API do Mercado Pago e só aprova o pedido quando ela está como processada.
- **Retorno do navegador não aprova pedidos:** as páginas `/compra/sucesso`, `/compra/pendente` e `/compra/falha` apenas mostram avisos. Quem aprova é o back-end.
- **Idempotência:** cada criação de order envia um `X-Idempotency-Key` único, e notificações repetidas não alteram pedidos que já saíram de "Aguardando pagamento".
- **Estoque consistente:** o checkout usa bloqueio pessimista nos livros e roda em uma única transação. Se o Mercado Pago recusar a order ou não devolver o link de pagamento, todo o pedido é desfeito e o estoque volta.
- **Um pedido pendente por vez:** enquanto houver um pedido aguardando pagamento, o carrinho fica bloqueado e um novo checkout é recusado com `409`.
- **Expiração:** pedidos não pagos expiram após 30 minutos, e os exemplares voltam ao estoque de venda.
- **Reembolsos:** só pedidos aprovados e ainda não retirados podem ser reembolsados, e apenas pelo dono do pedido ou automaticamente após 15 dias sem retirada.
- **Multas:** pagas pelo mesmo fluxo do Mercado Pago, com o status confirmado na API antes de serem dadas como pagas.

### 3.9 Infraestrutura
- **CORS** restrito às origens de `CORS_ALLOWED_ORIGINS`.
- Comunicação em **HTTPS** nos ambientes de produção (Render e Vercel).
- Erros inesperados retornam uma mensagem genérica; o detalhe fica apenas no log do servidor (`GlobalExceptionHandler`).
- Reservas e pedidos usam bloqueio pessimista no banco, impedindo condições de corrida no estoque e no limite de reservas.
- **Row Level Security** ativado em todas as tabelas da Supabase, sem políticas: as chaves públicas da Supabase não leem nem alteram dados, e só o back-end acessa o banco.
- Validação dos dados de entrada no back-end (`@Valid`), incluindo estoques e número de páginas não negativos.
- O link manual da capa só é aceito com `https`, até 255 caracteres e se apontar para uma imagem que existe.

---

## 4. Segredos e variáveis de ambiente

Nenhum segredo deve ser versionado. Todos são lidos de variáveis de ambiente:

| Variável | Conteúdo |
|---|---|
| `JWT_SECRET` | Chave de assinatura dos tokens. Use um valor aleatório com **pelo menos 32 caracteres**. |
| `CODIGO_FUNCIONARIO` | Código de cadastro de funcionários. |
| `SUPABASE_URL`, `SUPABASE_USER`, `SUPABASE_PASSWORD` | Acesso ao PostgreSQL. |
| `MONGODB_URI` | Acesso ao MongoDB de logs. |
| `BREVO_API_KEY`, `EMAIL_REMETENTE` | Envio de e-mails. |
| `BOOK_API` | Chave da Google Books API. |
| `MP_ACCESS_TOKEN` | Access Token do Mercado Pago. Em produção, permite criar cobranças reais em nome da biblioteca. |
| `MP_WEBHOOK_SECRET` | Assinatura secreta usada para validar o webhook do Mercado Pago. |

Boas práticas:
- Gere o `JWT_SECRET` com um gerador seguro, por exemplo: `openssl rand -base64 48`.
- Troque imediatamente qualquer segredo que tenha sido exposto (commit, print, log). Trocar o `JWT_SECRET` invalida todos os tokens emitidos.
- Use valores diferentes em desenvolvimento e em produção. No Mercado Pago, use as credenciais de **teste** fora da produção.
- Se o `MP_ACCESS_TOKEN` vazar, gere novas credenciais no painel do Mercado Pago. Se a assinatura do webhook vazar, gere outra no painel e atualize o `MP_WEBHOOK_SECRET`.
- Os arquivos `.env.development` e `.env.production` do front-end contêm apenas a URL pública da API e **não devem receber segredos**.

---

## 5. Para quem contribui

- Nunca faça commit de chaves, senhas ou arquivos `.env.local`.
- Toda rota nova deve ter a permissão definida explicitamente no `SecurityConfig`.
- Toda operação sobre dados de um usuário deve conferir se ele é o dono do recurso.
- Ações sensíveis devem gerar log pelo `LogAuditoriaService`, sem incluir senhas ou códigos.
- Nunca devolva o e-mail completo de outro usuário: use o `MascaraDeEmail` nas respostas que mostram dados de terceiros.
- Códigos enviados por e-mail devem ser gerados com `SecureRandom` e ter limite de tentativas.
- Senhas devem passar sempre pelo `PasswordEncoder`.
- Nunca confie em preço, status de pagamento ou dono de pedido vindos do front-end: leia do banco e confirme o pagamento na API do Mercado Pago.
- Toda notificação de pagamento deve ter a assinatura validada antes de qualquer processamento.
- Mantenha as dependências atualizadas e revise os alertas de segurança do GitHub (Dependabot).
