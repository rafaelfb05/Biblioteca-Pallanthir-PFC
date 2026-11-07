# Requisitos — Biblioteca Pallanthir

**Versão do documento:** 3.0
**Última atualização:** 07/11/2026

Este documento reúne os requisitos funcionais e não funcionais da **Biblioteca Pallanthir**, sistema de biblioteca voltado para estudantes de diversas áreas, que permite consultar o catálogo, avaliar, favoritar e reservar livros para retirada em uma biblioteca física, além de comprar exemplares físicos com pagamento pelo Mercado Pago.

Cada requisito possui um identificador único, uma prioridade e o seu status na versão atual.

| Prioridade | Significado |
|---|---|
| **Alta** | Essencial para o funcionamento do sistema. |
| **Média** | Importante, mas o sistema funciona sem ele. |
| **Baixa** | Desejável, pode ficar para versões futuras. |

| Status | Significado |
|---|---|
| ✅ Implementado | Disponível na versão atual. |
| 🟡 Parcial | Implementado com limitações. |
| ⏳ Planejado | Ainda não implementado. |

---

## 1. Atores

| Ator | Descrição |
|---|---|
| **Visitante** | Pessoa sem login. Pode navegar pelo catálogo e ler as avaliações. |
| **Estudante** | Usuário cadastrado como `ESTUDANTE`. Avalia, favorita, reserva e compra livros. |
| **Funcionário** | Usuário cadastrado como `FUNCIONARIO` mediante código de acesso. Gerencia livros, entregas, devoluções e consulta usuários, dashboard e logs. |
| **Mercado Pago** | Serviço externo que processa os pagamentos e notifica o sistema por webhook. |
| **Sistema** | Processos automáticos: envio de e-mails, registro de logs, expiração de sessão e de pedidos. |

---

## 2. Requisitos funcionais

### 2.1 Cadastro e gestão de conta

| ID | Requisito | Ator | Prioridade | Status |
|---|---|---|---|---|
| RF01 | O sistema deve permitir o cadastro de usuários com nome, e-mail, senha e tipo de conta (Estudante ou Funcionário). | Visitante | Alta | ✅ |
| RF02 | O sistema deve exigir um código de acesso válido (`CODIGO_FUNCIONARIO`) para o cadastro de contas do tipo Funcionário. | Visitante | Alta | ✅ |
| RF03 | O sistema deve exigir o aceite dos Termos de Uso e da Política de Privacidade no cadastro e registrar a data e a hora do aceite. | Visitante | Alta | ✅ |
| RF04 | O sistema deve impedir o cadastro de dois usuários ativos com o mesmo e-mail. | Sistema | Alta | ✅ |
| RF05 | O sistema deve permitir que o usuário atualize o próprio nome e e-mail. | Estudante / Funcionário | Média | ✅ |
| RF06 | O sistema deve permitir que o usuário exclua a própria conta, anonimizando os dados pessoais (nome, e-mail, senha e códigos). | Estudante / Funcionário | Alta | ✅ |
| RF07 | O sistema deve impedir a exclusão da conta enquanto houver reservas em aberto ou livros não devolvidos. | Sistema | Alta | ✅ |
| RF08 | O sistema deve permitir que o Funcionário liste os usuários ativos. | Funcionário | Média | ✅ |
| RF09 | O sistema deve exibir os Termos de Uso e a Política de Privacidade dentro da aplicação. | Visitante | Média | ✅ |

### 2.2 Autenticação

| ID | Requisito | Ator | Prioridade | Status |
|---|---|---|---|---|
| RF10 | O sistema deve permitir o login com e-mail e senha. | Visitante | Alta | ✅ |
| RF11 | O sistema deve exigir verificação em duas etapas (2FA) após a senha correta, enviando um código de 6 dígitos por e-mail, válido por 10 minutos. | Sistema | Alta | ✅ |
| RF12 | O sistema deve permitir reenviar o código de 2FA. | Visitante | Média | ✅ |
| RF13 | O sistema deve emitir um token JWT após a confirmação do código de 2FA. | Sistema | Alta | ✅ |
| RF14 | O sistema deve bloquear a conta por 15 minutos após 3 tentativas de login com senha incorreta. | Sistema | Alta | ✅ |
| RF15 | O sistema deve permitir a recuperação de senha por meio de um código de 6 dígitos enviado por e-mail, válido por 15 minutos. | Visitante | Alta | ✅ |
| RF16 | O sistema deve renovar o token automaticamente enquanto o usuário estiver ativo. | Sistema | Média | ✅ |
| RF17 | O sistema deve encerrar a sessão após 15 minutos sem atividade do usuário. | Sistema | Alta | ✅ |
| RF18 | O sistema deve permitir que o usuário encerre a sessão manualmente (logout). | Estudante / Funcionário | Alta | ✅ |
| RF19 | O e-mail com o código deve oferecer um link que abre uma página da aplicação para copiar o código com um clique. | Sistema | Baixa | ✅ |

### 2.3 Catálogo de livros

| ID | Requisito | Ator | Prioridade | Status |
|---|---|---|---|---|
| RF20 | O sistema deve permitir que o Funcionário cadastre livros a partir do título, preenchendo autores, ano, número de páginas e capa pela Google Books API. | Funcionário | Alta | ✅ |
| RF21 | O cadastro de livro deve permitir definir matéria, preço, estoque para reserva e estoque para venda. | Funcionário | Alta | ✅ |
| RF22 | O sistema deve permitir que o Funcionário atualize parcialmente os dados de um livro. | Funcionário | Alta | ✅ |
| RF23 | O sistema deve permitir que o Funcionário exclua livros por exclusão lógica, mantendo o histórico de reservas. | Funcionário | Alta | ✅ |
| RF24 | O sistema deve pedir confirmação antes de excluir um livro que possua reservas em aberto e removê-lo dos favoritos dos usuários. | Funcionário | Média | ✅ |
| RF25 | O sistema deve listar os livros ativos para qualquer pessoa, inclusive visitantes. | Visitante | Alta | ✅ |
| RF26 | O sistema deve permitir buscar livros pelo título. | Visitante | Alta | ✅ |
| RF27 | O sistema deve permitir filtrar livros por matéria (TI, Direito, Medicina, Odontologia, Veterinária, Física, Química, Arquitetura e Biologia), exibindo a quantidade de livros de cada uma. | Visitante | Média | ✅ |
| RF28 | O sistema deve exibir a página de detalhes do livro com capa ampliável, preço, estoque para reserva, estoque à venda e avaliações. | Visitante | Média | ✅ |
| RF29 | O sistema deve exibir um ranking com os 10 livros de maior nota média, considerando apenas livros com pelo menos 3 avaliações. | Visitante | Baixa | ✅ |
| RF30 | O sistema deve permitir que o Estudante avalie os livros com nota de 1 a 5 e comentário opcional de até 500 caracteres. | Estudante | Média | 🟡 Qualquer livro ativo pode ser avaliado; o sistema não confere se o estudante leu ou retirou o livro. |
| RF44 | Cada Estudante deve ter no máximo uma avaliação por livro; uma nova avaliação substitui a anterior. | Sistema | Média | ✅ |
| RF45 | O sistema deve permitir que o Estudante remova a própria avaliação. | Estudante | Baixa | ✅ |
| RF46 | O sistema deve exibir, para qualquer pessoa, a média das notas, o total de avaliações e os comentários de cada livro. | Visitante | Média | ✅ |
| RF47 | O sistema deve exibir uma capa genérica com o título quando a Google Books API não fornecer a capa do livro. | Sistema | Baixa | ✅ |

### 2.4 Favoritos

| ID | Requisito | Ator | Prioridade | Status |
|---|---|---|---|---|
| RF31 | O sistema deve permitir que o Estudante adicione e remova livros dos favoritos. | Estudante | Média | ✅ |
| RF32 | O sistema deve listar os livros favoritos do Estudante logado. | Estudante | Média | ✅ |

### 2.5 Reservas

| ID | Requisito | Ator | Prioridade | Status |
|---|---|---|---|---|
| RF33 | O sistema deve permitir que o Estudante reserve um livro com estoque para reserva disponível, diminuindo uma unidade desse estoque. | Estudante | Alta | ✅ |
| RF34 | O sistema deve impedir a reserva de livros sem estoque para reserva. | Sistema | Alta | ✅ |
| RF35 | O sistema deve permitir que o Estudante cancele uma reserva ainda não entregue, devolvendo a unidade ao estoque. | Estudante | Alta | ✅ |
| RF36 | O sistema deve permitir que o Funcionário registre a entrega do livro ao Estudante (`RESERVADO` → `ENTREGUE`). | Funcionário | Alta | ✅ |
| RF37 | O sistema deve permitir que o Funcionário registre a devolução do livro (`ENTREGUE` → `DEVOLVIDO`), devolvendo a unidade ao estoque. | Funcionário | Alta | ✅ |
| RF38 | O sistema deve listar o histórico de reservas do Estudante logado. | Estudante | Média | ✅ |
| RF39 | O sistema deve permitir que o Funcionário consulte as reservas de qualquer usuário. | Funcionário | Média | ✅ |

### 2.6 Auditoria

| ID | Requisito | Ator | Prioridade | Status |
|---|---|---|---|---|
| RF40 | O sistema deve registrar logs de auditoria das ações relevantes: cadastro, login, 2FA, recuperação de senha, alterações de conta, favoritos, reservas, livros, avaliações, pedidos, pagamentos e tentativas de acesso negado. | Sistema | Alta | ✅ |
| RF41 | O sistema deve permitir que o Funcionário consulte todos os logs, do mais recente ao mais antigo. | Funcionário | Média | ✅ |
| RF42 | O sistema deve permitir filtrar os logs por usuário e por texto. | Funcionário | Média | ✅ |

### 2.7 Carrinho e pagamentos

| ID | Requisito | Ator | Prioridade | Status |
|---|---|---|---|---|
| RF43 | O sistema deve permitir a compra de livros pela integração com o Mercado Pago (Checkout Pro). | Estudante | Alta | ✅ |
| RF48 | O sistema deve permitir que o Estudante adicione livros ao carrinho, altere a quantidade e remova itens. | Estudante | Alta | ✅ |
| RF49 | O sistema deve impedir que a quantidade no carrinho ultrapasse o estoque para venda do livro. | Sistema | Alta | ✅ |
| RF50 | Somente livros com estoque para venda e preço maior que zero podem ser adicionados ao carrinho. | Sistema | Alta | ✅ |
| RF51 | Ao finalizar a compra, o sistema deve criar um pedido, separar os exemplares do estoque para venda por 30 minutos e redirecionar o Estudante para o pagamento no Mercado Pago. | Sistema | Alta | ✅ |
| RF52 | O sistema deve receber as notificações de pagamento do Mercado Pago por webhook e marcar o pedido como aprovado quando o pagamento for processado. | Mercado Pago | Alta | ✅ |
| RF53 | O sistema deve consultar o Mercado Pago ao listar os pedidos, atualizando o status dos que ainda aguardam pagamento. | Sistema | Média | ✅ |
| RF54 | O sistema deve expirar pedidos não pagos após 30 minutos, devolvendo os exemplares ao estoque para venda. | Sistema | Alta | ✅ |
| RF55 | Os itens devem continuar no carrinho enquanto o pedido aguarda pagamento e sair dele quando o pedido for aprovado ou expirar. | Sistema | Média | ✅ |
| RF56 | Enquanto houver um pedido aguardando pagamento, o sistema deve bloquear alterações no carrinho e a criação de outro pedido, permitindo retomar o pagamento do pedido existente. | Sistema | Média | ✅ |
| RF57 | O sistema deve exibir ao Estudante a lista dos próprios pedidos, com itens, total, data, prazo de pagamento e status. | Estudante | Alta | ✅ |
| RF58 | Ao voltar do Mercado Pago, o sistema deve exibir um aviso conforme o resultado (sucesso, pendente ou falha) e atualizar o status do pedido automaticamente. | Sistema | Média | ✅ |

### 2.8 Dashboard

| ID | Requisito | Ator | Prioridade | Status |
|---|---|---|---|---|
| RF59 | O sistema deve exibir ao Funcionário o resumo do acervo: títulos ativos, exemplares para reserva, exemplares à venda e títulos esgotados para reserva. | Funcionário | Média | ✅ |
| RF60 | O sistema deve exibir ao Funcionário a quantidade de reservas por status. | Funcionário | Média | ✅ |
| RF61 | O sistema deve exibir ao Funcionário as 5 matérias mais reservadas e as 5 mais compradas. | Funcionário | Baixa | ✅ |

---

## 3. Requisitos não funcionais

### 3.1 Segurança

| ID | Requisito | Prioridade | Status |
|---|---|---|---|
| RNF01 | As senhas devem ser armazenadas apenas como hash **BCrypt**, nunca em texto puro. | Alta | ✅ |
| RNF02 | As senhas devem ter no mínimo 8 caracteres, com letra maiúscula, minúscula, número e caractere especial (`@ # $ % ^ & + = !`). | Alta | 🟡 Validado no cadastro; a redefinição de senha só é validada no front-end. |
| RNF03 | A autenticação deve ser *stateless*, por **JWT** assinado com HMAC-SHA e chave definida em `JWT_SECRET`. | Alta | ✅ |
| RNF04 | O token JWT deve expirar em 15 minutos. | Alta | ✅ |
| RNF05 | O acesso às rotas deve ser controlado por perfil (`ESTUDANTE` e `FUNCIONARIO`) pelo **Spring Security**. | Alta | ✅ |
| RNF06 | Um usuário só pode acessar e alterar os próprios dados, favoritos e reservas; tentativas contrárias devem ser bloqueadas e registradas. | Alta | ✅ |
| RNF07 | Mensagens de erro de login não devem indicar se o e-mail ou a senha estava incorreto. | Alta | ✅ |
| RNF08 | Códigos de 2FA e de recuperação devem expirar e ser apagados após o uso. | Alta | ✅ |
| RNF09 | Chaves, senhas e segredos devem ser lidos de variáveis de ambiente e nunca versionados. | Alta | ✅ |
| RNF10 | O CORS deve aceitar somente as origens configuradas em `CORS_ALLOWED_ORIGINS`. | Alta | ✅ |
| RNF11 | Erros inesperados não devem expor detalhes internos ao cliente. | Média | ✅ |
| RNF35 | O sistema não deve receber nem armazenar dados de cartão; o pagamento deve ocorrer na página do Mercado Pago. | Alta | ✅ |
| RNF36 | As notificações do webhook de pagamento devem ter a assinatura validada com HMAC-SHA256 e a chave `MP_WEBHOOK_SECRET`. | Alta | ✅ |
| RNF37 | O status de um pedido só deve mudar após confirmação na API do Mercado Pago, nunca pelo retorno do navegador ou pelo conteúdo da notificação. | Alta | ✅ |
| RNF38 | Os preços dos pedidos devem ser lidos do banco de dados, nunca enviados pelo front-end. | Alta | ✅ |

### 3.2 Privacidade (LGPD)

| ID | Requisito | Prioridade | Status |
|---|---|---|---|
| RNF12 | O sistema deve estar em conformidade com a **LGPD (Lei nº 13.709/2018)**, com Termos de Uso e Política de Privacidade publicados. | Alta | ✅ |
| RNF13 | O consentimento do usuário deve ser registrado com data e hora. | Alta | ✅ |
| RNF14 | A exclusão de conta deve anonimizar os dados pessoais, preservando apenas o necessário para o histórico de reservas, pedidos e auditoria. | Alta | ✅ |
| RNF15 | O código de acesso de funcionário não deve ser armazenado. | Média | ✅ |

### 3.3 Desempenho e confiabilidade

| ID | Requisito | Prioridade | Status |
|---|---|---|---|
| RNF16 | A reserva e o checkout devem usar bloqueio pessimista no banco (`buscarComLock`) para impedir que operações simultâneas consumam a mesma unidade. | Alta | ✅ |
| RNF39 | A criação do pedido deve ser atômica: se o Mercado Pago recusar a order ou não devolver o link de pagamento, o pedido inteiro deve ser desfeito e o estoque restaurado. | Alta | ✅ |
| RNF40 | A criação de orders no Mercado Pago deve enviar uma chave de idempotência (`X-Idempotency-Key`), e notificações repetidas não devem alterar pedidos já finalizados. | Média | ✅ |
| RNF41 | A expiração de pedidos deve rodar automaticamente a cada minuto. | Média | ✅ |
| RNF17 | A gravação de logs deve ser assíncrona e uma falha no MongoDB não deve interromper a operação principal. | Média | ✅ |
| RNF18 | O envio de e-mails deve ter tempo limite de conexão (5 s) e de leitura (10 s). | Média | ✅ |
| RNF19 | O back-end deve rodar com no máximo 400 MB de memória de heap, para caber no plano gratuito da Render. | Média | ✅ |
| RNF20 | Todas as datas devem usar o fuso horário `America/Sao_Paulo`. | Média | ✅ |

### 3.4 Usabilidade

| ID | Requisito | Prioridade | Status |
|---|---|---|---|
| RNF21 | A interface deve estar em português do Brasil. | Alta | ✅ |
| RNF22 | O sistema deve exibir mensagens de erro claras e em português. | Média | ✅ |
| RNF23 | A interface deve ser responsiva. | Média | 🟡 As páginas se adaptam ao celular, mas o menu principal fica oculto em telas de até 1000px; nelas, apenas o carrinho tem atalho no cabeçalho. |
| RNF24 | O usuário deve ser avisado quando a sessão for encerrada por inatividade ou expiração. | Média | ✅ |

### 3.5 Arquitetura e manutenibilidade

| ID | Requisito | Prioridade | Status |
|---|---|---|---|
| RNF25 | O back-end deve ser desenvolvido em **Java 24 + Spring Boot 4.1** com Maven. | Alta | ✅ |
| RNF26 | O front-end deve ser desenvolvido em **React**. | Alta | ✅ |
| RNF27 | Os dados de negócio devem ser persistidos em **PostgreSQL** (Supabase) e os logs de auditoria em **MongoDB**. | Alta | ✅ |
| RNF28 | A API deve ser documentada com **Swagger / OpenAPI** em `/swagger-ui.html`. | Média | ✅ |
| RNF29 | O código deve seguir a separação em camadas: controller, service, repository, entity e DTO. | Média | ✅ |
| RNF30 | Os erros devem ser tratados de forma centralizada (`GlobalExceptionHandler`), com códigos HTTP adequados (400, 401, 403, 404, 409, 500). | Média | ✅ |

### 3.6 Implantação

| ID | Requisito | Prioridade | Status |
|---|---|---|---|
| RNF31 | O back-end deve ser implantado na **Render** via `Dockerfile`. | Alta | ✅ |
| RNF32 | O front-end deve ser implantado na **Vercel**. | Alta | ✅ |
| RNF33 | Os e-mails transacionais devem ser enviados pela API da **Brevo**. | Alta | ✅ |
| RNF42 | Os pagamentos devem ser processados pela API de Orders do **Mercado Pago**, com credenciais lidas de `MP_ACCESS_TOKEN` e `MP_WEBHOOK_SECRET`. | Alta | ✅ |
| RNF34 | O esquema do banco relacional deve ser gerado automaticamente pelo Hibernate. | Média | ✅ |

---

## 4. Regras de negócio

| ID | Regra |
|---|---|
| RN01 | Cada e-mail só pode estar vinculado a uma conta ativa. |
| RN02 | Apenas quem possui o código de acesso de funcionário pode criar uma conta de Funcionário. |
| RN03 | Uma reserva segue o fluxo `RESERVADO` → `ENTREGUE` → `DEVOLVIDO`, ou `RESERVADO` → `CANCELADO`. |
| RN04 | Só é possível cancelar reservas com status `RESERVADO`. |
| RN05 | Só é possível entregar livros de reservas com status `RESERVADO` e devolver livros com status `ENTREGUE`. |
| RN06 | O estoque para reserva diminui ao reservar e aumenta ao cancelar ou devolver; a entrega não altera o estoque. |
| RN07 | Livros sem autor informado pela Google Books API recebem "Autor desconhecido". |
| RN08 | Livros excluídos deixam de aparecer no catálogo, mas continuam no histórico das reservas. |
| RN09 | Uma conta só pode ser excluída sem reservas em aberto nem livros pendentes de devolução. |
| RN10 | Cada livro tem dois estoques independentes: um para reserva e outro para venda. Livros novos começam com 1 exemplar para reserva e nenhum à venda. |
| RN11 | O estoque para venda diminui quando o pedido é criado e volta quando o pedido expira; a aprovação do pagamento não altera o estoque. |
| RN12 | Um pedido segue o fluxo `AGUARDANDO_PAGAMENTO` → `APROVADO`, ou `AGUARDANDO_PAGAMENTO` → `EXPIRADO`. |
| RN13 | Um pedido é aprovado quando a order correspondente no Mercado Pago está com status `processed`, e expira após 30 minutos sem pagamento (com 2 minutos de tolerância) ou quando a order é cancelada. |
| RN14 | Um pagamento aprovado em um pedido já expirado não reativa o pedido; ele é registrado no log como `PAGAMENTO_EM_PEDIDO_EXPIRADO` para tratamento manual. |
| RN15 | Cada Estudante pode ter apenas um pedido aguardando pagamento por vez. |
| RN16 | O preço de cada item do pedido é o preço do livro no momento da criação do pedido. |
| RN17 | O ranking de avaliações considera apenas livros ativos com pelo menos 3 avaliações. |
