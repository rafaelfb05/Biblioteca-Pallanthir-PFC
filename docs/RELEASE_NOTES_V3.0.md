# Biblioteca Pallanthir — v3.0

**Data de lançamento:** 07/11/2026

A versão 3.0 transforma a Pallanthir em uma biblioteca que também **vende livros físicos**. Os estudantes ganham **carrinho de compras**, **pagamento pelo Mercado Pago** e a página **Meus pedidos**, além de poderem **avaliar os livros** com nota e comentário. O estoque passa a ser **separado entre reserva e venda**, e os funcionários ganham uma **dashboard** com os números do acervo, das reservas e das vendas.

---

## Novidades

### Avaliações de livros
- Estudantes podem avaliar qualquer livro ativo do acervo com **nota de 1 a 5** e um **comentário opcional de até 500 caracteres**.
- Cada estudante tem **uma avaliação por livro**: avaliar de novo substitui a anterior.
- O estudante pode **remover a própria avaliação** a qualquer momento.
- A página do livro mostra a **média das notas**, o total de avaliações e a lista de comentários com o nome de quem avaliou.
- A página **Ranking** passa a listar os **10 livros mais bem avaliados**, considerando apenas livros com **pelo menos 3 avaliações**.
- As avaliações podem ser consultadas por qualquer pessoa, inclusive visitantes.

### Estoque separado para reserva e para venda
- O campo único `estoque` foi substituído por dois:
  - **`estoqueReserva`**: exemplares disponíveis para reserva e retirada;
  - **`estoqueCompra`**: exemplares disponíveis para venda.
- Os formulários de cadastro e de edição de livros têm um campo para cada estoque.
- A página do livro mostra as duas quantidades.
- Livros novos começam com **1 exemplar para reserva** e **nenhum à venda**. Um livro só aparece como disponível para compra quando tem estoque de venda e preço maior que zero.

### Carrinho de compras
- Botão **"Adicionar ao carrinho"** nos cards e na página de cada livro.
- Página **Meu carrinho**, com capa, preço unitário, controle de quantidade, subtotal, remoção de itens e total.
- O carrinho fica salvo no servidor, então acompanha o estudante em qualquer navegador.
- A quantidade nunca pode passar do estoque de venda do livro.

### Pagamento com o Mercado Pago (Checkout Pro)
- O botão **"Pagar com Mercado Pago"** cria um **pedido** e leva o estudante para a página de pagamento do Mercado Pago.
- Ao criar o pedido, os exemplares saem do estoque de venda e ficam **separados por 30 minutos**.
- **Os itens continuam no carrinho enquanto o pagamento está pendente** e só saem quando o pedido é **aprovado** ou **expira**.
- Enquanto houver um pedido aguardando pagamento, o carrinho fica **bloqueado** (não é possível adicionar, remover, mudar quantidades nem criar outro pedido). O botão de pagar passa a reabrir o pagamento do pedido em andamento.
- Depois de pagar, o Mercado Pago devolve o estudante ao site em `/compra/sucesso`, `/compra/pendente` ou `/compra/falha`, e a aplicação abre **Meus pedidos** com um aviso sobre o resultado.
- O status **Aprovado** nunca é definido pelo retorno do navegador: ele só muda quando o back-end confirma o pagamento com o Mercado Pago.

### Confirmação do pagamento
- Nova rota de **webhook** (`POST /pagamentos/webhook`), que recebe as notificações do Mercado Pago.
- Toda notificação tem a **assinatura validada** (HMAC-SHA256 com a assinatura secreta do webhook). Notificações sem assinatura válida são recusadas com `401`.
- Depois de validar a assinatura, o back-end **consulta o pedido diretamente na API do Mercado Pago** antes de alterar qualquer status.
- Como reforço, ao abrir **Meus pedidos**, o back-end consulta o Mercado Pago para cada pedido que ainda está aguardando pagamento. Assim, o status fica correto mesmo se uma notificação não chegar.
- Na volta do Mercado Pago, a página **Meus pedidos** atualiza o status sozinha a cada 5 segundos, por até 2 minutos.

### Meus pedidos
- Nova página com todos os pedidos do estudante: número, data, itens, total e status.
- Status possíveis: **Aguardando pagamento**, **Aprovado** e **Expirado**.
- Pedidos aguardando pagamento mostram o prazo e o botão **"Pagar com Mercado Pago"** para retomar o pagamento.

### Expiração de pedidos
- Um processo automático roda **a cada minuto** e expira os pedidos não pagos depois do prazo de 30 minutos (com 2 minutos de tolerância para pagamentos em andamento).
- Ao expirar, os exemplares **voltam ao estoque de venda** e os itens saem do carrinho.
- Pedidos cancelados pelo Mercado Pago também são expirados.
- Um pagamento aprovado em um pedido que já expirou não reativa o pedido: ele é registrado no log como `PAGAMENTO_EM_PEDIDO_EXPIRADO`, para tratamento manual.

### Dashboard para funcionários
- Nova página **Dashboard**, exclusiva para funcionários, com:
  - **Acervo:** títulos ativos, exemplares para reserva, exemplares à venda e títulos esgotados para reserva;
  - **Reservas:** quantidade de reservadas, entregues, devolvidas e canceladas, com a porcentagem de cada status;
  - **Matérias mais reservadas** e **matérias mais compradas** (top 5), em gráfico de barras.
- Botão **Atualizar** para recarregar os números.

### Sessão durante o pagamento
- O token de acesso é **renovado** logo antes de o estudante ir para o Mercado Pago, garantindo os 15 minutos completos de sessão durante o pagamento.
- Se a sessão terminar enquanto o estudante paga, a volta ao site mostra um aviso explicando que o pagamento não foi afetado e pede um novo login para ver o pedido.

### Logs de auditoria
- Novas ações registradas:
  - `AVALIACAO_REGISTRADA`, `AVALIACAO_REMOVIDA`
  - `PEDIDO_CRIADO`, `PAGAMENTO_APROVADO`, `PEDIDO_EXPIRADO`, `PAGAMENTO_EM_PEDIDO_EXPIRADO`

### Front-end
- Novas páginas **Meu carrinho**, **Meus pedidos** e **Dashboard**.
- Seção de avaliações na página do livro, com seletor de estrelas e contador de caracteres.
- **Capa genérica** com o título do livro quando a Google Books API não devolve uma capa.
- Atalho para o carrinho no cabeçalho em telas de até 1000px, onde o menu principal fica oculto.
- O item **"Livros"** foi removido do menu, já que apenas repetia o conteúdo da página inicial.

---

## Correções
- Corrigido o cadastro de livros, que ainda usava o campo antigo de estoque e impedia a compilação do back-end.
- Corrigida a consulta do ranking, que apontava para o pacote antigo do `RankingLivroDTO` e impedia a inicialização da aplicação.
- Corrigido o logger do `PedidoService`, que usava a classe errada.
- Removido o campo `unit_measure` dos itens enviados ao Mercado Pago, que fazia a API recusar todos os pedidos.
- Se o Mercado Pago não devolver o link de pagamento, o pedido é desfeito por completo: o estoque volta e o carrinho fica como estava.
- A assinatura do webhook passa a aceitar o ID do pedido em letras minúsculas, como o Mercado Pago calcula.
- Corrigido o botão **Reservar**, que aparecia como "Sem estoque" para todos os livros depois da separação dos estoques.
- Corrigido o erro de digitação "estoqe" na mensagem de estoque insuficiente do carrinho.
- Corrigido o teste automatizado do front-end, que falhava ao importar a biblioteca de Markdown.
- Removidos arquivos padrão não utilizados do Create React App.

---

## Migração do banco de dados

A coluna `estoque` da tabela `livros` foi substituída por `estoque_reserva` e `estoque_compra`. O Hibernate cria as colunas novas, mas os livros que já existiam ficam com os dois valores nulos. Depois do primeiro deploy da v3.0, rode no SQL Editor da Supabase:

```sql
UPDATE livros SET estoque_reserva = estoque WHERE estoque_reserva IS NULL;
UPDATE livros SET estoque_compra = 0 WHERE estoque_compra IS NULL;
```

Em seguida, um funcionário deve definir o **estoque para compra** de cada livro que estará à venda.

---

## Novas variáveis de ambiente

### Back-end

| Variável | Obrigatória | Descrição |
|---|---|---|
| `MP_ACCESS_TOKEN` | Sim | Access Token da aplicação no Mercado Pago, usado para criar e consultar pedidos. |
| `MP_WEBHOOK_SECRET` | Sim | Assinatura secreta do webhook, usada para validar as notificações do Mercado Pago. |

A variável `FRONTEND_URL` passa a ser usada também para montar os endereços de retorno do pagamento (`/compra/sucesso`, `/compra/pendente` e `/compra/falha`). As demais variáveis da v2.0 continuam valendo.

---

## Endpoints da API

### Novos

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| `POST` | `/avaliacoes/livro/{livroId}` | Estudante | Cria ou substitui a avaliação do livro |
| `GET` | `/avaliacoes/livro/{livroId}` | Público | Média, total e lista de avaliações do livro |
| `DELETE` | `/avaliacoes/livros/{avaliacaoId}` | Estudante | Remove a própria avaliação |
| `GET` | `/avaliacoes/ranking` | Público | 10 livros mais bem avaliados (mínimo de 3 avaliações) |
| `GET` | `/carrinho` | Estudante | Itens e total do carrinho |
| `POST` | `/carrinho/itens` | Estudante | Adiciona um livro ou define a quantidade (`livroId`, `quantidade`) |
| `DELETE` | `/carrinho/itens/{livroId}` | Estudante | Remove um livro do carrinho |
| `POST` | `/pedidos/checkout` | Estudante | Cria o pedido e devolve o link de pagamento |
| `GET` | `/pedidos` | Estudante | Lista os pedidos do estudante |
| `GET` | `/pedidos/{id}` | Estudante | Detalhes de um pedido do estudante |
| `POST` | `/pagamentos/webhook` | Mercado Pago (assinado) | Recebe as notificações de pagamento |
| `GET` | `/dashboard` | Funcionário | Números do acervo, das reservas e das vendas |

### Alterados

| Método | Rota | Mudança |
|---|---|---|
| `POST` | `/livros/cadastro` | O campo `estoque` foi substituído por `estoqueReserva` e `estoqueCompra`. |
| `PUT` | `/livros/atualizar/{id}` | Aceita `estoqueReserva` e `estoqueCompra`. |
| `GET` | `/livros` e demais consultas de livros | A resposta traz `estoqueReserva` e `estoqueCompra` no lugar de `estoque`. |

---

## Permissões

| Ação | Visitante | Estudante | Funcionário |
|---|:---:|:---:|:---:|
| Ver avaliações e ranking | ✅ | ✅ | ✅ |
| Avaliar livros e remover a própria avaliação | | ✅ | |
| Usar o carrinho, comprar e ver os próprios pedidos | | ✅ | |
| Ver a dashboard | | | ✅ |

As permissões da v2.0 continuam valendo.

---

## Como testar os pagamentos

1. Configure `MP_ACCESS_TOKEN` com o Access Token de **teste** da aplicação no Mercado Pago e `MP_WEBHOOK_SECRET` com a assinatura secreta do webhook de teste.
2. No painel do Mercado Pago, configure o webhook com a URL `https://<sua-api>/pagamentos/webhook` e o evento **Order (Mercado Pago)**.
3. Crie uma **conta compradora de teste** em **Suas integrações → Contas de teste**.
4. Em uma janela anônima, entre no Mercado Pago com a conta compradora de teste e, depois, faça a compra pelo site.
5. Pague com um cartão de teste e o titular **`APRO`** para simular um pagamento aprovado.

Pagar sem estar logado com a conta compradora de teste, ou usar o e-mail de uma conta real, gera o erro "Uma das partes com as quais você está tentando efetuar o pagamento é de teste".

---

## Como executar
Consulte o [README](README.md) para os pré-requisitos e o passo a passo de execução local e deploy, e esta página para as novas variáveis de ambiente e a migração do banco.
