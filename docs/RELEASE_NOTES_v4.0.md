# Biblioteca Pallanthir — v4.0

**Data de lançamento:** 22/11/2026

A versão 4.0 cria o perfil **Gerente**, que passa a cuidar da equipe, dos documentos legais, da dashboard de vendas e dos logs. As reservas ganham uma **política com aceite registrado**, **prazos de retirada e devolução**, **limite de reservas** e **multa por dano**. As compras ganham **cancelamento**, **reembolso** e **retirada na biblioteca**. Os estudantes podem pedir um **aviso por e-mail** quando um livro esgotado voltar ao estoque. A capa dos livros passa a vir da **Open Library**, os identificadores passam a ser **UUID**, e a versão traz uma rodada de correções de **segurança e de privacidade**: os e-mails passam a aparecer **escondidos** fora do perfil de cada pessoa.

---

## Novidades

### Perfil Gerente
- Novo tipo de conta **Gerente**, que tem todos os acessos do Funcionário e mais:
  - **Equipe:** promover funcionários a gerente, rebaixar gerentes a funcionário e excluir contas de funcionários;
  - **Documentos:** editar os Termos de Uso, a Política de Privacidade e a Política de Reservas pelo próprio site;
  - **Vendas:** dashboard de vendas;
  - **Logs:** consulta dos logs de auditoria, que deixa de estar disponível para os funcionários.
- **Não é possível se cadastrar como Gerente.** O primeiro gerente é definido direto no banco (veja [Migração do banco de dados](#migração-do-banco-de-dados)) e os próximos são promovidos por ele na página **Equipe**.
- **O sistema nunca fica sem gerente:** não é possível rebaixar nem excluir o último gerente ativo, e o último gerente também não consegue excluir a própria conta. Dois gerentes rebaixando um ao outro ao mesmo tempo também não zeram os gerentes (bloqueio pessimista no banco).
- O gerente não altera o próprio perfil nem exclui a própria conta pela página Equipe.
- Menu do gerente na ordem: **Início, Matérias, Ranking, Gestão, Equipe, Dashboard, Vendas, Documentos e Logs**. O menu do funcionário segue a mesma ordem, sem os itens exclusivos do gerente.

### Documentos editáveis e histórico de versões
- Os Termos de Uso, a Política de Privacidade e a Política de Reservas ficam guardados no **banco de dados** e podem ser editados pelo gerente na página **Documentos**, com **pré-visualização em Markdown**.
- Cada alteração guarda a **versão anterior no histórico**, com data e nome de quem alterou. O gerente pode abrir qualquer versão antiga.
- Na primeira inicialização, se o banco ainda não tiver os documentos, eles são criados a partir dos arquivos de `backend/src/main/resources/documentos`.

### Novo aceite quando os termos mudam
- A **versão aceita** dos Termos de Uso e da Política de Privacidade passa a ser registrada na conta, além da data e da hora.
- Quando um desses documentos é alterado, o usuário vê uma janela pedindo o **novo aceite** ao entrar no site. Ele precisa abrir os dois documentos antes de aceitar.
- Enquanto a versão nova não for aceita, **não é possível reservar nem finalizar compras**.
- A versão é calculada da mesma forma no front-end e no back-end, a partir do texto dos documentos.
- No cadastro, os Termos e a Política viraram links dentro do texto da caixa de aceite.

### Política de Reservas
- Antes de cada reserva, o estudante lê e aceita a **Política de Uso de Reservas**. A reserva guarda a **data, a hora e a versão** da política aceita.
- Uma reserva só é feita com a versão atual da política. Se ela tiver mudado, o site mostra a nova versão para aceite.

### Prazos e limites das reservas
- **Retirada:** 3 dias depois da reserva. Se o livro não for retirado, a reserva é **cancelada automaticamente** e o exemplar volta ao estoque.
- **Devolução:** 7 dias depois da retirada.
- Os dois prazos aparecem em **Minhas reservas** e na **Gestão**.
- **Bloqueio por atraso:** enquanto houver um livro com a devolução atrasada, o estudante não consegue fazer novas reservas.
- **Limite:** uma reserva em aberto de cada livro e até **3 reservas em aberto** ao mesmo tempo, contando livros reservados e livros com o estudante.
- Só é possível reservar livros ativos.
- **Minhas reservas** agora mostra a capa do livro e lista as reservas da mais recente para a mais antiga.

### Multa por dano
- Na devolução, o funcionário pode marcar o livro como **danificado**. É aplicada uma **multa no valor do exemplar**, ou no valor que o funcionário informar, com uma observação de até 300 caracteres. O livro danificado sai de circulação.
- A multa aparece em **Minha conta** e em um **aviso no topo do site**, e é paga pelo **Mercado Pago**. O link de pagamento vale **60 minutos**.
- Enquanto houver multa pendente, o estudante **não consegue reservar nem comprar** e não pode excluir a conta.
- O funcionário vê as multas pendentes na **Gestão** e pode **dispensar** uma multa registrada por engano.

### Compras: cancelamento, retirada e reembolso
- O estudante pode **cancelar** um pedido que ainda aguarda pagamento. Os exemplares voltam ao estoque.
- Depois de aprovada, a compra deve ser **retirada na biblioteca em até 15 dias**. O prazo aparece em **Meus pedidos**.
- O funcionário registra a retirada na aba **Compras para retirar** da Gestão. O pedido passa para **Entregue**.
- Enquanto o livro não foi retirado, o estudante pode **pedir o reembolso** pelo site. O valor é devolvido pelo Mercado Pago e os exemplares voltam ao estoque.
- Compras não retiradas em 15 dias são **reembolsadas automaticamente**.
- Novos status de pedido: **Entregue**, **Cancelado** e **Reembolsado**.

### Aviso de disponibilidade
- Quando um livro está esgotado, aparece um **sino** ao lado de cada estoque (reserva e venda). Ao clicar, o estudante confirma que quer ser avisado.
- Quando o estoque volta, o estudante recebe um **e-mail com a capa do livro** e um link que abre a página do livro no site.
- Os avisos são enviados por um processo automático **a cada 2 minutos**, com até **5 tentativas** em caso de falha no envio.
- O estudante pode cancelar o aviso clicando de novo no sino. Os avisos são cancelados quando a conta é excluída.

### Capas dos livros
- A capa passa a ser buscada na **Open Library** pelo ISBN. Se não houver capa pelo ISBN, a busca é feita pelo título e pelo autor.
- O funcionário pode informar um **link da capa** na edição do livro. O link precisa ser `https`, ter no máximo 255 caracteres e apontar para uma imagem que existe.
- Se nada for encontrado, o site mostra uma **capa genérica** nova, limpa e centralizada, com o título do livro.
- A página do livro mostra e permite editar o **ISBN-10** e o **ISBN-13**.

### Acervo
- O cadastro de um livro que **já está no acervo é bloqueado**. Para ter mais exemplares, o funcionário edita o estoque do livro existente.
- Os estoques e o número de páginas não aceitam valores negativos.
- O acervo é dividido em **páginas de 18 livros**.
- O **Ranking** pode ser filtrado por matéria.
- O card do livro mostra **"Reservado"** quando o estudante já tem uma reserva em aberto daquele livro.
- Novo **menu de navegação em gaveta**, aberto pelo botão no cabeçalho.

### Dashboard de vendas
- Nova página **Vendas**, exclusiva do gerente, com filtro de período:
  - **Resumo:** faturamento, pedidos pagos, ticket médio, exemplares vendidos, compras aguardando retirada, compras entregues e reembolsos;
  - **Pedidos por status**;
  - **Livros e matérias mais vendidos**;
  - **Vendas por mês**;
  - **Multas:** recebidas e pendentes.
- Os números de vendas contam os pedidos **aprovados e entregues**. A dashboard dos funcionários também passou a contar os dois status nas matérias mais compradas.

### Logs de auditoria
- **Somente o gerente** consulta os logs.
- A tabela mostra o **nome do usuário** e é dividida em **páginas de 50 registros**.
- Filtros por **usuário, ação e período**, e busca por nome, ação, palavra da descrição ou e-mail completo.
- Os logs são **apagados automaticamente após 12 meses** (índice TTL no MongoDB).
- Novas ações registradas, entre elas:
  - `ACEITE_TERMOS`, `ALTERACAO_PERFIL`, `DOCUMENTO_ATUALIZADO`, `EXCLUSAO_FUNCIONARIO`
  - `AVISO_DISPONIBILIDADE_CRIADO`, `AVISO_DISPONIBILIDADE_ENVIADO`, `AVISO_DISPONIBILIDADE_CANCELADO`
  - `DEVOLUCAO_COM_DANO`, `MULTA_APLICADA`, `MULTA_PAGA`, `MULTA_DISPENSADA`
  - `PEDIDO_CANCELADO`, `PEDIDO_ENTREGUE`, `PEDIDO_REEMBOLSADO`, `PEDIDO_REEMBOLSADO_AUTOMATICO`
  - `RESERVA_EXPIRADA`, `SOLICITACAO_TROCA_EMAIL`, `TROCA_EMAIL`, `VISUALIZOU_EMAIL`

### E-mails escondidos
- Fora da página **Minha conta**, os e-mails aparecem **parcialmente escondidos** (por exemplo, `fe•••@gmail.com`): na Gestão, na Equipe e nos Logs.
- O e-mail é escondido **no back-end**, então o endereço completo nem chega ao navegador.
- Nos Logs, o gerente pode ver o e-mail completo clicando no **ícone de olho**. Cada visualização é registrada como `VISUALIZOU_EMAIL`, sem o e-mail no texto do registro.
- As buscas da Equipe e da Gestão passam a ser feitas pelo nome. Nos Logs, a busca por e-mail funciona com o e-mail completo, sem mostrá-lo.

### Troca de e-mail com confirmação
- Ao trocar o e-mail em **Minha conta**, o sistema envia um **código para o novo endereço**. A troca só acontece depois que o código é confirmado, e o usuário precisa entrar de novo.
- O código vale **15 minutos** e é invalidado depois de **5 tentativas erradas**.

### Identificadores UUID
- Os identificadores numéricos de todas as tabelas foram trocados por **UUID**. Os scripts da migração estão em [`docs/migracao-uuid`](migracao-uuid).

### Integração contínua
- Novo workflow do **GitHub Actions**, que compila e testa o back-end e testa e gera o build do front-end a cada push e a cada pull request para a `main`.

### Segurança
- Os códigos de 2FA, de recuperação de senha e de troca de e-mail são gerados com **`SecureRandom`** e comparados em **tempo constante**.
- Cada código é **invalidado depois de 5 tentativas erradas**.
- Um novo código de recuperação só pode ser pedido depois de **1 minuto**.
- As respostas de recuperação de senha e de código errado são **genéricas** ("Se o e-mail estiver cadastrado…", "Código inválido ou expirado"), sem indicar se a conta existe.
- O filtro de autenticação passa a **carregar o usuário do banco em cada requisição**. Um token deixa de valer na hora em que a conta é excluída, o e-mail é trocado ou o perfil muda.
- A checagem de estoque e de limite das reservas usa **bloqueio pessimista**, evitando que duas reservas simultâneas passem do limite.
- Os campos de estoque e de número de páginas passam por validação no back-end.
- O **RLS (Row Level Security)** foi ativado em todas as tabelas da Supabase, sem políticas. Só o back-end, que usa o dono do banco, acessa os dados.

---

## Correções

- Corrigida a dashboard de vendas, que mostrava matérias mais compradas diferentes da dashboard dos funcionários.
- Corrigida a busca dos logs, que não encontrava usuários pelo nome nem livros pelo título.
- Corrigida a capa do e-mail de aviso de disponibilidade, que não aparecia quando o link era do Google Books.
- Corrigidas as caixas de texto preenchidas automaticamente pelo navegador, que ficavam brancas no tema escuro.
- O botão **"Ver minhas reservas"** da janela de exclusão de conta agora só aparece para estudantes.
- **Minhas reservas** passou a listar da mais recente para a mais antiga.
- As janelas de confirmação (excluir livro, remover avaliação, avisos de disponibilidade e livro duplicado) passaram a usar o mesmo modelo visual.
- Avisos de prazo mais compactos, posicionados embaixo dos itens nas páginas de reservas e de gestão.
- O código do 2FA no e-mail foi ajustado.
- Os limites de memória da JVM no `Dockerfile` foram reduzidos para caber no plano gratuito da Render.

---

## Migração do banco de dados

O Hibernate cria as tabelas e colunas novas sozinho (`ddl-auto=update`), e a aplicação atualiza na inicialização as restrições de status e de tipo de usuário. Depois do primeiro deploy da v4.0:

1. **Identificadores UUID:** se o banco veio da v3.0, rode os scripts de `docs/migracao-uuid` na ordem (`1-backup.sql`, `2-migracao.sql` e `3-verificacao.sql`).
2. **Primeiro gerente:** promova uma conta de funcionário no SQL Editor da Supabase:

   ```sql
   UPDATE usuarios SET tipo = 'GERENTE' WHERE email = 'email-do-gerente@exemplo.com' AND ativo = true;
   ```

   Os próximos gerentes são promovidos pela página **Equipe**.
3. **RLS:** ative o Row Level Security em todas as tabelas, sem criar políticas.

Os documentos legais são criados no banco na primeira inicialização. Depois disso, as alterações são feitas pela página **Documentos**, e não pelos arquivos do repositório.

---

## Endpoints da API

### Novos

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| `GET` | `/documentos` | Público | Lista os documentos legais |
| `GET` | `/documentos/{tipo}` | Público | Um documento (`TERMOS_DE_USO`, `POLITICA_DE_PRIVACIDADE` ou `POLITICA_DE_RESERVAS`) |
| `PUT` | `/documentos/{tipo}` | Gerente | Altera um documento e guarda a versão anterior |
| `GET` | `/documentos/{tipo}/versoes` | Gerente | Histórico de versões do documento |
| `GET` | `/documentos/versoes/{id}` | Gerente | Uma versão antiga |
| `GET` | `/usuarios/termos` | Autenticado | Versão e data dos termos aceitos pelo usuário |
| `POST` | `/usuarios/termos/aceite` | Autenticado | Registra o aceite da versão atual dos termos |
| `POST` | `/usuarios/confirmar-email` | Autenticado | Confirma a troca de e-mail com o código |
| `PUT` | `/usuarios/{id}/perfil` | Gerente | Promove ou rebaixa um membro da equipe |
| `DELETE` | `/usuarios/funcionarios/{id}` | Gerente | Exclui a conta de um funcionário |
| `GET` | `/dashboard/vendas?de&ate` | Gerente | Números de vendas do período |
| `GET` | `/logs/acoes` | Gerente | Ações existentes nos logs, para o filtro |
| `GET` | `/logs/{id}/email` | Gerente | Mostra o e-mail completo de um log e registra a visualização |
| `GET` | `/reservas/em-aberto` | Funcionário | Reservas reservadas e entregues, com prazos |
| `GET` | `/multas/minhas` | Estudante | Multas do estudante |
| `POST` | `/multas/{id}/pagar` | Estudante | Gera o link de pagamento da multa |
| `GET` | `/multas` | Funcionário | Multas pendentes |
| `PUT` | `/multas/{id}/dispensar` | Funcionário | Dispensa uma multa |
| `POST` | `/pedidos/{id}/cancelar` | Estudante | Cancela um pedido que aguarda pagamento |
| `POST` | `/pedidos/{id}/reembolso` | Estudante | Pede o reembolso de uma compra ainda não retirada |
| `GET` | `/pedidos/pendentes-retirada` | Funcionário | Compras pagas aguardando retirada |
| `PUT` | `/pedidos/{id}/entregar` | Funcionário | Registra a retirada de uma compra |
| `GET` | `/avisos/meus` | Estudante | Avisos de disponibilidade do estudante |
| `POST` | `/avisos` | Estudante | Cria um aviso (`livroId`, `tipo`: `RESERVA` ou `COMPRA`) |
| `DELETE` | `/avisos/{id}` | Estudante | Cancela um aviso |

### Alterados

| Método | Rota | Mudança |
|---|---|---|
| Todas | Todas as rotas com `{id}` | Os identificadores passam a ser UUID. |
| `GET` | `/logs` e `/logs/usuarios/{id}` | Exclusivas do gerente. Aceitam os filtros `acao`, `de`, `ate` e `limite`. Os e-mails vêm escondidos, com `emailOculto` e `chavesDeEmail` para a busca. |
| `GET` | `/usuarios` | Os e-mails vêm escondidos. |
| `PUT` | `/usuarios/atualizar/{id}` | A troca de e-mail fica pendente até a confirmação do código (`emailPendente`). |
| `POST` | `/reservas/{usuarioId}/{livroId}` | Exige `politicaVersao` com a versão atual da Política de Reservas e os termos aceitos. |
| `PUT` | `/reservas/{id}/devolver` | Aceita `danificado`, `valorMulta` e `observacao` para aplicar a multa por dano. |
| `POST` | `/pedidos/checkout` | Exige os termos aceitos e nenhuma multa pendente. |
| `POST` | `/livros/cadastro` | Recusa livros já cadastrados com `409` e valores negativos com `400`. |
| `PUT` | `/livros/atualizar/{id}` | Aceita `isbn10`, `isbn13` e `capaUrl`. |
| `GET` | `/avaliacoes/ranking` | Aceita o filtro `materia`. |

---

## Permissões

| Ação | Visitante | Estudante | Funcionário | Gerente |
|---|:---:|:---:|:---:|:---:|
| Ler os documentos legais | ✅ | ✅ | ✅ | ✅ |
| Pedir aviso de disponibilidade | | ✅ | | |
| Pagar multas, cancelar pedidos e pedir reembolso | | ✅ | | |
| Gestão de reservas, compras para retirar e multas | | | ✅ | ✅ |
| Dashboard geral | | | ✅ | ✅ |
| Logs de auditoria | | | | ✅ |
| Dashboard de vendas | | | | ✅ |
| Editar documentos e ver o histórico | | | | ✅ |
| Promover, rebaixar e excluir funcionários | | | | ✅ |

As permissões da v3.0 continuam valendo, exceto a consulta dos logs, que passou do funcionário para o gerente.

---

## Como executar
Consulte o [README](README.md) para os pré-requisitos e o passo a passo de execução local e deploy, e esta página para a migração do banco.
