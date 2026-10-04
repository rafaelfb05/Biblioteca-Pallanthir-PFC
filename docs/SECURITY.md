# Política de Segurança — Biblioteca Pallanthir

**Última atualização:** 04/10/2026

Este documento descreve as versões que recebem correções de segurança, como relatar uma vulnerabilidade e quais mecanismos de proteção a **Biblioteca Pallanthir** utiliza.

Para saber como tratamos os seus dados pessoais, consulte a [Política de Privacidade](POLITICA_DE_PRIVACIDADE.md) e os [Termos de Uso](TERMOS_DE_USO.md).

---

## 1. Versões suportadas

| Versão | Suportada | Observação |
|---|:---:|---|
| 2.0 | ✅ | Versão atual, com Spring Security, JWT, 2FA e BCrypt. |
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
- **Dentro do escopo:** o back-end deste repositório, o front-end hospedado na Vercel e os fluxos de cadastro, login, 2FA, recuperação de senha, reservas e logs.
- **Fora do escopo:** serviços de terceiros (Supabase, MongoDB, Render, Vercel, Brevo, Google Books), ataques de negação de serviço, engenharia social e testes que acessem ou alterem dados de outros usuários reais.

---

## 3. Mecanismos de segurança

### 3.1 Senhas
- Armazenadas **somente como hash BCrypt**, com *salt* aleatório por senha (`PasswordEncoderConfig`).
- A senha original nunca é gravada, registrada em log ou devolvida pela API.
- Política de senha forte: mínimo de 8 caracteres, com letra maiúscula, minúscula, número e caractere especial (`@ # $ % ^ & + = !`).

### 3.2 Autenticação
- **Verificação em duas etapas (2FA):** após a senha correta, um código de 6 dígitos é enviado por e-mail. O código vale **10 minutos** e é apagado após o uso.
- **JWT:** após o 2FA, a API emite um token assinado com HMAC-SHA (`JWT_SECRET`), com **validade de 15 minutos**, enviado no cabeçalho `Authorization: Bearer <token>`.
- **API stateless:** não há sessão no servidor (`SessionCreationPolicy.STATELESS`).
- **Renovação automática:** o front-end renova o token quando faltam menos de 5 minutos para expirar, apenas se o usuário estiver ativo.
- **Logout por inatividade:** a sessão é encerrada após **15 minutos sem atividade**.

### 3.3 Proteção contra força bruta
- Após **3 tentativas** de senha incorreta, a conta é **bloqueada por 15 minutos**.
- A mensagem de erro do login é genérica ("Email ou senha incorretos").

### 3.4 Recuperação de senha
- Código de 6 dígitos enviado por e-mail, válido por **15 minutos** e apagado após o uso.

### 3.5 Autorização
- Controle de acesso por perfil no **Spring Security** (`SecurityConfig`):
  - **Público:** catálogo de livros, cadastro, login, 2FA e recuperação de senha.
  - **Estudante:** favoritos, reservas e cancelamentos.
  - **Funcionário:** cadastro, edição e exclusão de livros, entrega e devolução, lista de usuários e logs.
- **Verificação de propriedade:** além do perfil, o serviço confere se o recurso pertence ao usuário logado (conta, favoritos e reservas). Tentativas contrárias são negadas e registradas como `ACESSO_NEGADO`.
- O cadastro de Funcionário exige o código `CODIGO_FUNCIONARIO`, que não é armazenado.

### 3.6 Auditoria
- Ações sensíveis são registradas no **MongoDB** (coleção `logs_auditoria`): cadastros, logins, falhas de login, bloqueios, 2FA, recuperação de senha, alterações em livros, reservas e acessos negados.
- Os logs **não contêm senhas, hashes nem códigos** de verificação.
- Somente funcionários podem consultar os logs.

### 3.7 Privacidade (LGPD)
- Aceite dos Termos de Uso e da Política de Privacidade registrado com data e hora.
- Exclusão de conta com **anonimização**: nome, e-mail, senha, códigos e favoritos são removidos ou substituídos.

### 3.8 Infraestrutura
- **CORS** restrito às origens de `CORS_ALLOWED_ORIGINS`.
- Comunicação em **HTTPS** nos ambientes de produção (Render e Vercel).
- Erros inesperados retornam uma mensagem genérica; o detalhe fica apenas no log do servidor (`GlobalExceptionHandler`).
- Reservas usam bloqueio pessimista no banco, impedindo condições de corrida no estoque.

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

Boas práticas:
- Gere o `JWT_SECRET` com um gerador seguro, por exemplo: `openssl rand -base64 48`.
- Troque imediatamente qualquer segredo que tenha sido exposto (commit, print, log). Trocar o `JWT_SECRET` invalida todos os tokens emitidos.
- Use valores diferentes em desenvolvimento e em produção.
- Os arquivos `.env.development` e `.env.production` do front-end contêm apenas a URL pública da API e **não devem receber segredos**.

---

## 5. Para quem contribui

- Nunca faça commit de chaves, senhas ou arquivos `.env.local`.
- Toda rota nova deve ter a permissão definida explicitamente no `SecurityConfig`.
- Toda operação sobre dados de um usuário deve conferir se ele é o dono do recurso.
- Ações sensíveis devem gerar log pelo `LogAuditoriaService`, sem incluir senhas ou códigos.
- Senhas devem passar sempre pelo `PasswordEncoder`.
- Mantenha as dependências atualizadas e revise os alertas de segurança do GitHub (Dependabot).
