# Requisitos — Biblioteca Pallanthir

**Versão do documento:** 4.0
**Última atualização:** 22/11/2026

Este documento reúne os requisitos funcionais e não funcionais da **Biblioteca Pallanthir**, sistema de biblioteca voltado para estudantes de diversas áreas, que permite consultar o catálogo, avaliar, favoritar e reservar livros para retirada em uma biblioteca física, comprar exemplares físicos com pagamento pelo Mercado Pago e receber avisos quando um livro esgotado volta ao estoque. A equipe da biblioteca controla prazos, retiradas, devoluções e multas, e o gerente cuida da equipe, dos documentos legais, das vendas e da auditoria.

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
| **Funcionário** | Usuário cadastrado como `FUNCIONARIO` mediante código de acesso. Gerencia livros, entregas, devoluções, retiradas de compras e multas, e consulta usuários e a dashboard. |
| **Gerente** | Funcionário promovido a `GERENTE`. Tem os acessos do Funcionário e também consulta os logs e a dashboard de vendas, edita os documentos legais e gerencia a equipe. |
| **Mercado Pago** | Serviço externo que processa os pagamentos e os reembolsos e notifica o sistema por webhook. |
| **Sistema** | Processos automáticos: envio de e-mails, registro de logs, expiração de sessão, de pedidos e de reservas, reembolsos por prazo e avisos de disponibilidade. |

---

## 2. Requisitos funcionais

### 2.1 Cadastro e gestão de conta

| ID | Requisito | Ator | Prioridade | Status |
|---|---|---|---|---|
| RF01 | O sistema deve permitir o cadastro de usuários com nome, e-mail, senha e tipo de conta (Estudante ou Funcionário). | Visitante | Alta | ✅ |
| RF02 | O sistema deve exigir um código de acesso válido (`CODIGO_FUNCIONARIO`) para o cadastro de contas do tipo Funcionário. | Visitante | Alta | ✅ |
| RF03 | O sistema deve exigir o aceite dos Termos de Uso e da Política de Privacidade no cadastro e registrar a data, a hora e a versão aceita. | Visitante | Alta | ✅ |
| RF04 | O sistema deve impedir o cadastro de dois usuários ativos com o mesmo e-mail. | Sistema | Alta | ✅ |
| RF05 | O sistema deve permitir que o usuário atualize o próprio nome e e-mail; a troca de e-mail só vale depois da confirmação de um código enviado ao novo endereço. | Estudante / Funcionário | Média | ✅ |
| RF06 | O sistema deve permitir que o usuário exclua a própria conta, anonimizando os dados pessoais (nome, e-mail, senha e códigos). | Estudante / Funcionário | Alta | ✅ |
| RF07 | O sistema deve impedir a exclusão da conta enquanto houver reservas em aberto, livros não devolvidos, pedidos aguardando pagamento ou retirada ou multas pendentes. | Sistema | Alta | ✅ |
| RF08 | O sistema deve permitir que o Funcionário liste os usuários ativos, com o e-mail parcialmente escondido. | Funcionário | Média | ✅ |
| RF09 | O sistema deve exibir os Termos de Uso e a Política de Privacidade dentro da aplicação, carregados do banco de dados. | Visitante | Média | ✅ |

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
| RF20 | O sistema deve permitir que o Funcionário cadastre livros a partir do título, preenchendo autores, ano, número de páginas e ISBN pela Google Books API e a capa pela Open Library. | Funcionário | Alta | ✅ |
| RF21 | O cadastro de livro deve permitir definir matéria, preço, estoque para reserva e estoque para venda. | Funcionário | Alta | ✅ |
| RF22 | O sistema deve permitir que o Funcionário atualize parcialmente os dados de um livro. | Funcionário | Alta | ✅ |
| RF23 | O sistema deve permitir que o Funcionário exclua livros por exclusão lógica, mantendo o histórico de reservas. | Funcionário | Alta | ✅ |
| RF24 | O sistema deve pedir confirmação antes de excluir um livro que possua reservas em aberto e removê-lo dos favoritos dos usuários. | Funcionário | Média | ✅ |
| RF25 | O sistema deve listar os livros ativos para qualquer pessoa, inclusive visitantes, em páginas de 18 livros. | Visitante | Alta | ✅ |
| RF26 | O sistema deve permitir buscar livros pelo título. | Visitante | Alta | ✅ |
| RF27 | O sistema deve permitir filtrar livros por matéria (TI, Direito, Medicina, Odontologia, Veterinária, Física, Química, Arquitetura e Biologia), exibindo a quantidade de livros de cada uma. | Visitante | Média | ✅ |
| RF28 | O sistema deve exibir a página de detalhes do livro com capa ampliável, preço, estoque para reserva, estoque à venda e avaliações. | Visitante | Média | ✅ |
| RF29 | O sistema deve exibir um ranking com os 10 livros de maior nota média, considerando apenas livros com pelo menos 3 avaliações, com filtro por matéria. | Visitante | Baixa | ✅ |
| RF30 | O sistema deve permitir que o Estudante avalie os livros com nota de 1 a 5 e comentário opcional de até 500 caracteres. | Estudante | Média | 🟡 Qualquer livro ativo pode ser avaliado; o sistema não confere se o estudante leu ou retirou o livro. |
| RF44 | Cada Estudante deve ter no máximo uma avaliação por livro; uma nova avaliação substitui a anterior. | Sistema | Média | ✅ |
| RF45 | O sistema deve permitir que o Estudante remova a própria avaliação. | Estudante | Baixa | ✅ |
| RF46 | O sistema deve exibir, para qualquer pessoa, a média das notas, o total de avaliações e os comentários de cada livro. | Visitante | Média | ✅ |
| RF47 | O sistema deve exibir uma capa genérica com o título quando nenhuma capa for encontrada para o livro. | Sistema | Baixa | ✅ |
| RF62 | O sistema deve buscar a capa na Open Library pelo ISBN e, se não encontrar, pelo título e pelo autor. | Sistema | Média | ✅ |
| RF63 | O sistema deve permitir que o Funcionário informe um link https para a capa do livro, validando se a imagem existe. | Funcionário | Baixa | ✅ |
| RF64 | O sistema deve permitir que o Funcionário veja e edite o ISBN-10 e o ISBN-13 do livro. | Funcionário | Baixa | ✅ |
| RF65 | O sistema deve impedir o cadastro de um livro que já esteja no acervo, orientando a editar o estoque do livro existente. | Sistema | Média | ✅ |

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
| RF66 | O sistema deve exigir, antes de cada reserva, o aceite da versão atual da Política de Uso de Reservas, registrando a data, a hora e a versão aceita. | Estudante | Alta | ✅ |
| RF67 | O sistema deve cancelar automaticamente as reservas não retiradas em 3 dias, devolvendo a unidade ao estoque. | Sistema | Alta | ✅ |
| RF68 | O sistema deve exibir os prazos de retirada e de devolução (7 dias após a retirada) ao Estudante e ao Funcionário. | Sistema | Média | ✅ |
| RF69 | O sistema deve impedir novas reservas enquanto o Estudante tiver uma devolução atrasada. | Sistema | Alta | ✅ |
| RF70 | O sistema deve limitar o Estudante a uma reserva em aberto por livro e a 3 reservas em aberto ao mesmo tempo. | Sistema | Alta | ✅ |
| RF71 | O sistema deve permitir que o Funcionário registre a devolução de um livro danificado, aplicando uma multa no valor do exemplar ou no valor informado. | Funcionário | Alta | ✅ |
| RF72 | O sistema deve permitir que o Estudante pague a multa pelo Mercado Pago e impedir reservas, compras e a exclusão da conta enquanto houver multa pendente. | Estudante | Alta | ✅ |
| RF73 | O sistema deve permitir que o Funcionário consulte as multas pendentes e dispense uma multa registrada por engano. | Funcionário | Média | ✅ |

### 2.6 Auditoria

| ID | Requisito | Ator | Prioridade | Status |
|---|---|---|---|---|
| RF40 | O sistema deve registrar logs de auditoria das ações relevantes: cadastro, login, 2FA, recuperação de senha, alterações de conta, favoritos, reservas, livros, avaliações, pedidos, pagamentos e tentativas de acesso negado. | Sistema | Alta | ✅ |
| RF41 | O sistema deve permitir que o Gerente consulte os logs, do mais recente ao mais antigo, com o nome do usuário e páginas de 50 registros. | Gerente | Média | ✅ |
| RF42 | O sistema deve permitir filtrar os logs por usuário, ação e período e buscá-los por nome, ação, palavra da descrição ou e-mail completo. | Gerente | Média | ✅ |
| RF74 | O sistema deve exibir os e-mails dos logs parcialmente escondidos e permitir que o Gerente veja o e-mail completo, registrando cada visualização nos logs. | Gerente | Média | ✅ |

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
| RF75 | O sistema deve permitir que o Estudante cancele um pedido que ainda aguarda pagamento, devolvendo os exemplares ao estoque. | Estudante | Média | ✅ |
| RF76 | O sistema deve permitir que o Funcionário consulte as compras pagas aguardando retirada e registre a retirada. | Funcionário | Alta | ✅ |
| RF77 | O sistema deve permitir que o Estudante peça o reembolso de uma compra aprovada que ainda não foi retirada. | Estudante | Alta | ✅ |
| RF78 | O sistema deve reembolsar automaticamente as compras não retiradas em 15 dias após a aprovação do pagamento. | Sistema | Média | ✅ |

### 2.8 Aviso de disponibilidade

| ID | Requisito | Ator | Prioridade | Status |
|---|---|---|---|---|
| RF79 | O sistema deve permitir que o Estudante peça um aviso quando um livro esgotado voltar ao estoque para reserva ou para venda, e cancelar esse aviso. | Estudante | Baixa | ✅ |
| RF80 | O sistema deve enviar, quando o estoque voltar, um e-mail com a capa do livro e um link para a página dele, uma vez por aviso. | Sistema | Baixa | ✅ |

### 2.9 Dashboards

| ID | Requisito | Ator | Prioridade | Status |
|---|---|---|---|---|
| RF59 | O sistema deve exibir ao Funcionário o resumo do acervo: títulos ativos, exemplares para reserva, exemplares à venda e títulos esgotados para reserva. | Funcionário | Média | ✅ |
| RF60 | O sistema deve exibir ao Funcionário a quantidade de reservas por status. | Funcionário | Média | ✅ |
| RF61 | O sistema deve exibir ao Funcionário as 5 matérias mais reservadas e as 5 mais compradas. | Funcionário | Baixa | ✅ |
| RF81 | O sistema deve exibir ao Gerente a dashboard de vendas do período escolhido: faturamento, pedidos pagos, ticket médio, exemplares vendidos, pedidos por status, livros e matérias mais vendidos, vendas por mês, reembolsos e multas. | Gerente | Média | ✅ |

### 2.10 Gerente, equipe e documentos

| ID | Requisito | Ator | Prioridade | Status |
|---|---|---|---|---|
| RF82 | O sistema deve permitir que o Gerente promova funcionários a gerente e rebaixe gerentes a funcionário. | Gerente | Alta | ✅ |
| RF83 | O sistema deve permitir que o Gerente exclua contas de funcionários, anonimizando os dados. | Gerente | Média | ✅ |
| RF84 | O sistema deve manter sempre pelo menos um gerente ativo, impedindo rebaixar ou excluir o último. | Sistema | Alta | ✅ |
| RF85 | O sistema deve impedir o cadastro direto como Gerente. | Sistema | Alta | ✅ |
| RF86 | O sistema deve permitir que o Gerente edite os Termos de Uso, a Política de Privacidade e a Política de Reservas pela aplicação. | Gerente | Média | ✅ |
| RF87 | O sistema deve guardar o histórico de versões dos documentos, com data e autor, e permitir consultar cada versão. | Gerente | Baixa | ✅ |
| RF88 | Quando os Termos de Uso ou a Política de Privacidade mudarem, o sistema deve pedir um novo aceite e bloquear reservas e compras até que ele seja feito. | Sistema | Alta | ✅ |
| RF89 | O sistema deve exibir os e-mails parcialmente escondidos em todas as telas, exceto na página da própria conta. | Sistema | Média | ✅ |

---

## 3. Requisitos não funcionais

### 3.1 Segurança

| ID | Requisito | Prioridade | Status |
|---|---|---|---|
| RNF01 | As senhas devem ser armazenadas apenas como hash BCrypt, nunca em texto puro. | Alta | ✅ |
| RNF02 | As senhas devem ter no mínimo 8 caracteres, com letra maiúscula, minúscula, número e caractere especial (`@ # $ % ^ & + = !`). | Alta | 🟡 Validado no cadastro; a redefinição de senha só é validada no front-end. |
| RNF03 | A autenticação deve ser stateless, por JWT assinado com HMAC-SHA e chave definida em `JWT_SECRET`. | Alta | ✅ |
| RNF04 | O token JWT deve expirar em 15 minutos. | Alta | ✅ |
| RNF05 | O acesso às rotas deve ser controlado por perfil (`ESTUDANTE`, `FUNCIONARIO` e `GERENTE`) pelo Spring Security. | Alta | ✅ |
| RNF06 | Um usuário só pode acessar e alterar os próprios dados, favoritos, reservas, pedidos, multas e avisos; tentativas contrárias devem ser bloqueadas e registradas. | Alta | ✅ |
| RNF07 | Mensagens de erro de login não devem indicar se o e-mail ou a senha estava incorreto. | Alta | ✅ |
| RNF08 | Códigos de 2FA, de recuperação e de troca de e-mail devem ser gerados com `SecureRandom`, expirar, ser apagados após o uso e ser invalidados após 5 tentativas erradas. | Alta | ✅ |
| RNF09 | Chaves, senhas e segredos devem ser lidos de variáveis de ambiente e nunca versionados. | Alta | ✅ |
| RNF10 | O CORS deve aceitar somente as origens configuradas em `CORS_ALLOWED_ORIGINS`. | Alta | ✅ |
| RNF11 | Erros inesperados não devem expor detalhes internos ao cliente. | Média | ✅ |
| RNF35 | O sistema não deve receber nem armazenar dados de cartão; o pagamento deve ocorrer na página do Mercado Pago. | Alta | ✅ |
| RNF36 | As notificações do webhook de pagamento devem ter a assinatura validada com HMAC-SHA256 e a chave `MP_WEBHOOK_SECRET`. | Alta | ✅ |
| RNF37 | O status de um pedido só deve mudar após confirmação na API do Mercado Pago, nunca pelo retorno do navegador ou pelo conteúdo da notificação. | Alta | ✅ |
| RNF38 | Os preços dos pedidos devem ser lidos do banco de dados, nunca enviados pelo front-end. | Alta | ✅ |
| RNF43 | A cada requisição, o sistema deve conferir no banco se a conta do token continua ativa, com o mesmo e-mail e o mesmo perfil. | Alta | ✅ |
| RNF44 | As respostas de recuperação de senha e de código inválido não devem indicar se o e-mail está cadastrado. | Alta | ✅ |
| RNF45 | O e-mail completo de outros usuários não deve ser enviado ao navegador; o back-end deve escondê-lo antes de responder. | Alta | ✅ |
| RNF46 | As tabelas do banco relacional devem ter Row Level Security ativado, com acesso apenas pelo back-end. | Média | ✅ |

### 3.2 Privacidade (LGPD)

| ID | Requisito | Prioridade | Status |
|---|---|---|---|
| RNF12 | O sistema deve estar em conformidade com a LGPD (Lei nº 13.709/2018), com Termos de Uso e Política de Privacidade publicados. | Alta | ✅ |
| RNF13 | O consentimento do usuário deve ser registrado com data, hora e versão dos documentos aceitos. | Alta | ✅ |
| RNF14 | A exclusão de conta deve anonimizar os dados pessoais, preservando apenas o necessário para o histórico de reservas, pedidos e auditoria. | Alta | ✅ |
| RNF15 | O código de acesso de funcionário não deve ser armazenado. | Média | ✅ |
| RNF47 | Os logs de auditoria devem ser apagados automaticamente 12 meses após o registro. | Média | ✅ |

### 3.3 Desempenho e confiabilidade

| ID | Requisito | Prioridade | Status |
|---|---|---|---|
| RNF16 | A reserva e o checkout devem usar bloqueio pessimista no banco (`buscarComLock`) para impedir que operações simultâneas consumam a mesma unidade ou passem do limite de reservas. | Alta | ✅ |
| RNF48 | A troca de perfil e a exclusão de gerentes devem usar bloqueio pessimista para que operações simultâneas não deixem o sistema sem gerente. | Média | ✅ |
| RNF49 | Os prazos de reservas e de retirada de compras devem ser verificados automaticamente a cada 15 minutos, e os avisos de disponibilidade a cada 2 minutos, com até 5 tentativas de envio. | Média | ✅ |
| RNF39 | A criação do pedido deve ser atômica: se o Mercado Pago recusar a order ou não devolver o link de pagamento, o pedido inteiro deve ser desfeito e o estoque restaurado. | Alta | ✅ |
| RNF40 | A criação de orders no Mercado Pago deve enviar uma chave de idempotência (`X-Idempotency-Key`), e notificações repetidas não devem alterar pedidos já finalizados. | Média | ✅ |
| RNF41 | A expiração de pedidos deve rodar automaticamente a cada minuto. | Média | ✅ |
| RNF17 | A gravação de logs deve ser assíncrona e uma falha no MongoDB não deve interromper a operação principal. | Média | ✅ |
| RNF18 | O envio de e-mails deve ter tempo limite de conexão (5 s) e de leitura (10 s). | Média | ✅ |
| RNF19 | O back-end deve rodar com no máximo 256 MB de memória de heap, para caber no plano gratuito da Render. | Média | ✅ |
| RNF20 | Todas as datas devem usar o fuso horário `America/Sao_Paulo`. | Média | ✅ |

### 3.4 Usabilidade

| ID | Requisito | Prioridade | Status |
|---|---|---|---|
| RNF21 | A interface deve estar em português do Brasil. | Alta | ✅ |
| RNF22 | O sistema deve exibir mensagens de erro claras e em português. | Média | ✅ |
| RNF23 | A interface deve ser responsiva. | Média | ✅ |
| RNF24 | O usuário deve ser avisado quando a sessão for encerrada por inatividade ou expiração. | Média | ✅ |

### 3.5 Arquitetura e manutenibilidade

| ID | Requisito | Prioridade | Status |
|---|---|---|---|
| RNF25 | O back-end deve ser desenvolvido em Java 24 + Spring Boot 4.1 com Maven. | Alta | ✅ |
| RNF26 | O front-end deve ser desenvolvido em React. | Alta | ✅ |
| RNF27 | Os dados de negócio devem ser persistidos em PostgreSQL (Supabase) e os logs de auditoria em MongoDB. | Alta | ✅ |
| RNF28 | A API deve ser documentada com Swagger / OpenAPI em `/swagger-ui.html`. | Média | ✅ |
| RNF29 | O código deve seguir a separação em camadas: controller, service, repository, entity e DTO. | Média | ✅ |
| RNF30 | Os erros devem ser tratados de forma centralizada (`GlobalExceptionHandler`), com códigos HTTP adequados (400, 401, 403, 404, 409, 500). | Média | ✅ |

### 3.6 Implantação

| ID | Requisito | Prioridade | Status |
|---|---|---|---|
| RNF31 | O back-end deve ser implantado na Render via Dockerfile. | Alta | ✅ |
| RNF32 | O front-end deve ser implantado na Vercel. | Alta | ✅ |
| RNF33 | Os e-mails transacionais devem ser enviados pela API da Brevo. | Alta | ✅ |
| RNF42 | Os pagamentos devem ser processados pela API de Orders do Mercado Pago, com credenciais lidas de `MP_ACCESS_TOKEN` e `MP_WEBHOOK_SECRET`. | Alta | ✅ |
| RNF34 | O esquema do banco relacional deve ser gerado automaticamente pelo Hibernate. | Média | ✅ |
| RNF50 | Cada push e cada pull request para a main devem passar pela integração contínua do GitHub Actions, com testes e build do back-end e do front-end. | Média | ✅ |
| RNF51 | Os identificadores das entidades devem ser UUID. | Média | ✅ |

---

## 4. Regras de negócio

| ID | Regra |
|---|---|
| RN01 | Cada e-mail só pode estar vinculado a uma conta ativa. |
| RN02 | Apenas quem possui o código de acesso de funcionário pode criar uma conta de Funcionário. |
| RN03 | Uma reserva segue o fluxo `RESERVADO` → `ENTREGUE` → `DEVOLVIDO`, ou `RESERVADO` → `CANCELADO` (pelo estudante ou automaticamente após 3 dias sem retirada). |
| RN04 | Só é possível cancelar reservas com status `RESERVADO`. |
| RN05 | Só é possível entregar livros de reservas com status `RESERVADO` e devolver livros com status `ENTREGUE`. |
| RN06 | O estoque para reserva diminui ao reservar e aumenta ao cancelar ou devolver; a entrega não altera o estoque. |
| RN07 | Livros sem autor informado pela Google Books API recebem "Autor desconhecido". |
| RN08 | Livros excluídos deixam de aparecer no catálogo, mas continuam no histórico das reservas. |
| RN09 | Uma conta só pode ser excluída sem reservas em aberto nem livros pendentes de devolução. |
| RN10 | Cada livro tem dois estoques independentes: um para reserva e outro para venda. Livros novos começam com 1 exemplar para reserva e nenhum à venda. |
| RN11 | O estoque para venda diminui quando o pedido é criado e volta quando o pedido expira, é cancelado ou é reembolsado; a aprovação do pagamento e a retirada não alteram o estoque. |
| RN12 | Um pedido segue o fluxo `AGUARDANDO_PAGAMENTO` → `APROVADO` → `ENTREGUE` ou `REEMBOLSADO`, ou `AGUARDANDO_PAGAMENTO` → `EXPIRADO` ou `CANCELADO`. |
| RN13 | Um pedido é aprovado quando a order correspondente no Mercado Pago está com status `processed`, e expira após 30 minutos sem pagamento (com 2 minutos de tolerância) ou quando a order é cancelada. |
| RN14 | Um pagamento aprovado em um pedido já expirado não reativa o pedido; ele é registrado no log como `PAGAMENTO_EM_PEDIDO_EXPIRADO` para tratamento manual. |
| RN15 | Cada Estudante pode ter apenas um pedido aguardando pagamento por vez. |
| RN16 | O preço de cada item do pedido é o preço do livro no momento da criação do pedido. |
| RN17 | O ranking de avaliações considera apenas livros ativos com pelo menos 3 avaliações. |
| RN18 | O livro reservado deve ser retirado em até 3 dias e devolvido em até 7 dias após a retirada. |
| RN19 | Um estudante pode ter uma reserva em aberto por livro e até 3 reservas em aberto ao mesmo tempo; reservas `RESERVADO` e `ENTREGUE` contam como em aberto. |
| RN20 | Um estudante com devolução atrasada ou multa pendente não pode fazer novas reservas; com multa pendente, também não pode comprar. |
| RN21 | A multa por dano tem o valor do exemplar, ou o valor informado pelo funcionário, e o livro danificado sai de circulação. |
| RN22 | Uma compra aprovada deve ser retirada em até 15 dias; depois disso, é reembolsada automaticamente. Só compras aprovadas e não retiradas podem ser reembolsadas. |
| RN23 | Só pedidos aguardando pagamento podem ser cancelados pelo estudante. |
| RN24 | Reservas e compras exigem o aceite da versão atual dos Termos de Uso e da Política de Privacidade; reservas exigem também a versão atual da Política de Reservas. |
| RN25 | Não é possível se cadastrar como Gerente; um gerente é promovido por outro, e o sistema mantém sempre pelo menos um gerente ativo. |
| RN26 | O Gerente não pode alterar o próprio perfil nem excluir a própria conta pela página Equipe. |
| RN27 | Não pode haver dois livros ativos com o mesmo ISBN; quando o livro não tem ISBN, a comparação é feita pelo título e pelos autores. |
| RN28 | Um aviso de disponibilidade só pode ser pedido para um estoque esgotado e é enviado uma única vez. |
