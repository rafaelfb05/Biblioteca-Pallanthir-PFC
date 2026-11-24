# Termos de Uso — Biblioteca Pallanthir

**Última atualização:** 22/11/2026

Estes Termos de Uso regulam o acesso e a utilização da **Biblioteca Pallanthir**, disponível em [pallanthir.vercel.app](https://pallanthir.vercel.app). Ao criar uma conta ou utilizar o sistema, você declara que leu, compreendeu e concorda com estes Termos e com a [Política de Privacidade](./POLITICA_DE_PRIVACIDADE.md).

Se você não concordar com algum ponto, não utilize o sistema.

---

## 1. Sobre o sistema

A Biblioteca Pallanthir é um sistema de biblioteca voltado para estudantes de diversas áreas, desenvolvido como biblioteca virtual. O sistema permite:

- consultar o acervo de livros, filtrar por matéria e ver o ranking dos mais bem avaliados;
- avaliar livros com nota e comentário;
- favoritar livros;
- reservar livros para retirada em uma **biblioteca física**;
- comprar **exemplares físicos** de livros, com pagamento pelo **Mercado Pago**;
- receber um **aviso por e-mail** quando um livro esgotado voltar ao estoque.

O sistema **não disponibiliza livros em formato digital**. As reservas servem apenas para separar exemplares físicos, e as compras se referem a exemplares físicos do acervo.

Por se tratar de um projeto em desenvolvimento, o sistema pode passar por mudanças e instabilidades.

---

## 2. Tipos de conta

| Tipo | O que pode fazer |
|---|---|
| **Visitante** (sem conta) | Consultar o acervo, as matérias, as avaliações e o ranking. |
| **Estudante** | Tudo o que o visitante faz, além de avaliar livros, favoritar livros, reservar livros, cancelar as próprias reservas e comprar livros. |
| **Funcionário** | Cadastrar, editar e excluir livros; consultar as reservas e as compras dos estudantes; registrar entregas, devoluções e retiradas de compras; aplicar e dispensar multas por dano; consultar a dashboard. |
| **Gerente** | Tudo o que o funcionário faz, além de consultar os logs de auditoria e a dashboard de vendas, editar os Termos de Uso, a Política de Privacidade e a Política de Reservas, promover e rebaixar membros da equipe e excluir contas de funcionários. |

O cadastro como **Funcionário** exige um **código de acesso** fornecido pela administração da biblioteca. É proibido criar conta de funcionário sem autorização ou compartilhar esse código.

Não é possível se cadastrar como **Gerente**: um gerente é sempre promovido por outro gerente, a partir de uma conta de funcionário. O sistema mantém sempre pelo menos um gerente ativo.

---

## 3. Cadastro e conta

Para criar uma conta, você deve informar nome, e-mail e senha. Você se compromete a:

- fornecer informações verdadeiras e mantê-las atualizadas;
- utilizar um e-mail ao qual tenha acesso, pois ele é necessário para o login e para a recuperação de senha;
- criar uma senha com no mínimo 8 caracteres, contendo letra maiúscula, letra minúscula, número e um caractere especial;
- manter sua senha e os códigos recebidos por e-mail em sigilo;
- não criar contas em nome de terceiros.

Para concluir o cadastro, é obrigatório marcar a opção **"Li e aceito os Termos de Uso e a Política de Privacidade"**. Os dois documentos podem ser lidos no próprio formulário de cadastro, clicando no nome de cada um, e ficam disponíveis a qualquer momento em **Minha conta → Documentos**. A data, a hora e a versão dos documentos aceitos são registradas na sua conta.

Para trocar o e-mail da conta, é preciso confirmar um código enviado para o **novo endereço**. Até a confirmação, a conta continua usando o e-mail antigo.

Você é responsável por todas as ações realizadas com a sua conta.

---

## 4. Acesso e segurança

Para proteger as contas, o sistema adota as seguintes medidas:

- **Verificação em duas etapas:** após informar e-mail e senha, um código de 6 dígitos é enviado para o seu e-mail. O código vale por **10 minutos**.
- **Bloqueio por tentativas:** após **3 tentativas de login com senha incorreta**, a conta fica bloqueada por **15 minutos**.
- **Recuperação de senha:** feita por um código enviado ao e-mail cadastrado, válido por **15 minutos**. Um novo código só pode ser pedido depois de 1 minuto.
- **Limite de tentativas nos códigos:** os códigos de verificação, de recuperação de senha e de troca de e-mail deixam de valer após **5 tentativas erradas**, e é preciso pedir um novo.
- **Encerramento por inatividade:** a sessão é encerrada automaticamente após **15 minutos sem atividade**.
- **Registro de ações:** ações relevantes (logins, cadastros, reservas, avaliações, pedidos, pagamentos, alterações no acervo, tentativas de acesso negado etc.) são registradas em logs de auditoria, conforme a [Política de Privacidade](./POLITICA_DE_PRIVACIDADE.md).

Se suspeitar de uso indevido da sua conta, altere sua senha imediatamente e entre em contato pelo e-mail indicado na seção 13.

---

## 5. Reservas

- Só é possível reservar livros que tenham exemplares disponíveis no **estoque para reserva**.
- Ao reservar, uma unidade do livro fica separada para você.
- Uma reserva pode ter os seguintes status:
  - **Reservado:** a unidade está separada, aguardando a retirada na biblioteca;
  - **Entregue:** o livro foi retirado e está em posse do estudante;
  - **Devolvido:** o livro foi devolvido à biblioteca;
  - **Cancelado:** a reserva foi cancelada antes da retirada.
- O estudante só pode **cancelar** uma reserva enquanto ela estiver como **Reservado**, e a unidade volta ao estoque. Depois que o livro é entregue, a reserva não pode mais ser cancelada.
- A **entrega** é feita presencialmente na biblioteca, e somente um funcionário pode registrá-la no sistema.
- A **devolução** também é feita presencialmente na biblioteca e registrada por um funcionário no sistema.
- Antes de cada reserva, é preciso ler e aceitar a **[Política de Uso de Reservas](./POLITICA_DE_RESERVAS.md)**, que define as regras abaixo e complementa estes Termos. A data, a hora e a versão da política aceita ficam registradas na reserva.
- **Prazos:** o livro deve ser retirado em até **3 dias** depois da reserva, ou a reserva é cancelada automaticamente. Depois da retirada, ele deve ser devolvido em até **7 dias**. Os prazos aparecem em **Minhas reservas**.
- **Limites:** é possível ter uma reserva em aberto de cada livro e até **3 reservas em aberto** ao mesmo tempo, contando os livros reservados e os que estão com você.
- **Atraso:** enquanto houver um livro com a devolução atrasada, não é possível fazer novas reservas.
- **Danos e perda:** se o livro for danificado ou perdido enquanto estiver com você, é aplicada uma **multa no valor do exemplar**, paga pelo Mercado Pago no próprio site. Enquanto houver multa pendente, não é possível reservar nem comprar livros.

---

## 6. Avaliações

- Estudantes podem avaliar os livros do acervo com uma **nota de 1 a 5** e um **comentário opcional de até 500 caracteres**.
- Cada estudante pode ter **uma avaliação por livro**. Avaliar o mesmo livro de novo substitui a avaliação anterior.
- As avaliações são **públicas**: a nota, o comentário, a data e o **seu nome** ficam visíveis para qualquer pessoa que acesse a página do livro, inclusive visitantes sem conta.
- Você pode remover a sua avaliação a qualquer momento, pela página do livro.
- A média das notas é usada no ranking dos livros mais bem avaliados, que considera apenas livros com pelo menos 3 avaliações.
- Você é responsável pelo conteúdo que publica. A equipe do projeto pode remover avaliações que violem estes Termos.

---

## 7. Compras

### 7.1 Carrinho e pedido
- Só é possível comprar livros que tenham exemplares no **estoque para venda** e preço definido. Os preços são exibidos em reais (R$).
- Ao clicar em **"Pagar com Mercado Pago"**, é criado um **pedido** com os itens do carrinho, e os exemplares ficam **separados para você por 30 minutos**.
- O preço de cada item é o preço do livro no momento em que o pedido é criado.
- Os itens continuam no carrinho enquanto o pedido aguarda pagamento e saem dele quando o pedido é aprovado ou expira.
- Enquanto houver um pedido aguardando pagamento, não é possível alterar o carrinho nem criar outro pedido. Você pode retomar o pagamento pelo carrinho ou pela página **Meus pedidos**, ou cancelar o pedido, e os exemplares voltam ao estoque.
- Não é possível finalizar uma compra com uma **multa pendente** ou sem ter aceitado a versão atual destes Termos e da Política de Privacidade.

### 7.2 Pagamento
- O pagamento é feito **na página do Mercado Pago**, e as formas de pagamento disponíveis são as oferecidas por ele. O uso do Mercado Pago está sujeito aos termos e à política de privacidade do próprio Mercado Pago.
- A Biblioteca Pallanthir **não recebe nem armazena os dados do seu cartão** ou da sua conta bancária.
- O pedido só é considerado **aprovado** quando o Mercado Pago confirma o pagamento ao sistema. A confirmação pode levar alguns instantes, e o status pode ser acompanhado em **Meus pedidos**.

### 7.3 Status do pedido
- **Aguardando pagamento:** o pedido foi criado e os exemplares estão separados, aguardando a confirmação do pagamento.
- **Aprovado:** o pagamento foi confirmado e os livros aguardam a retirada na biblioteca.
- **Entregue:** os livros foram retirados na biblioteca.
- **Cancelado:** você cancelou o pedido antes de pagar. Nenhum valor é cobrado.
- **Expirado:** o pagamento não foi confirmado em 30 minutos ou foi cancelado pelo Mercado Pago. Os exemplares voltam ao estoque e nenhum valor é cobrado por esse pedido.
- **Reembolsado:** o valor pago foi devolvido pelo Mercado Pago.

### 7.4 Retirada, arrependimento e reembolso
- Os livros comprados são **exemplares físicos** e devem ser retirados na biblioteca em até **15 dias** depois da aprovação do pagamento. O prazo aparece em **Meus pedidos**.
- Compras que não forem retiradas nesse prazo são **reembolsadas automaticamente**, e os exemplares voltam ao estoque.
- Enquanto os livros não forem retirados, você pode **pedir o reembolso** pela página **Meus pedidos**. O valor é devolvido pelo Mercado Pago, no mesmo meio de pagamento, conforme os prazos dele.
- Se um pagamento for aprovado depois que o pedido expirou, o caso é tratado manualmente pela equipe, que fará a entrega do livro, se houver estoque, ou o reembolso.
- Você pode desistir da compra em até **7 dias** após a compra ou a retirada, conforme o art. 49 do Código de Defesa do Consumidor. Pedidos de arrependimento depois da retirada ou problemas com uma compra devem ser enviados pelo e-mail de contato da seção 13, com o número do pedido.

### 7.5 Aviso de disponibilidade
- Quando um livro estiver esgotado para reserva ou para venda, o estudante pode clicar no **sino** e pedir para ser avisado.
- Quando o estoque voltar, o aviso é enviado **uma vez** para o seu e-mail, com a capa do livro e um link para a página dele. O aviso **não reserva nem separa** o exemplar.
- Você pode cancelar o aviso a qualquer momento clicando de novo no sino.

---

## 8. Regras de uso

Ao utilizar o sistema, você se compromete a **não**:

- tentar acessar contas, dados, pedidos ou funções que não sejam os seus, inclusive alterando identificadores nas requisições;
- tentar burlar a verificação em duas etapas, o bloqueio por tentativas, a validação de pagamentos ou qualquer outro mecanismo de segurança;
- realizar reservas ou pedidos em massa, falsos ou com a intenção de impedir que outras pessoas reservem ou comprem;
- publicar avaliações falsas, ofensivas, discriminatórias, com dados pessoais de terceiros ou sem relação com o livro;
- enviar conteúdo ilegal, ofensivo ou que viole direitos de terceiros;
- usar meios de pagamento que não sejam seus ou sem autorização do titular;
- sobrecarregar, atacar ou tentar interromper o funcionamento do sistema;
- usar o sistema para qualquer finalidade contrária à lei.

Tentativas de acesso não autorizado são registradas nos logs de auditoria.

---

## 9. Suspensão e exclusão de conta

- A administração pode suspender ou encerrar contas que violem estes Termos.
- Você pode **excluir sua conta** em **Minha conta → Excluir minha conta**, desde que não tenha reservas com status **Reservado** ou **Entregue**, pedidos aguardando pagamento ou aguardando retirada, nem multas pendentes. Nesses casos, resolva a pendência antes de solicitar a exclusão. O único gerente ativo do sistema também não pode excluir a própria conta antes de promover outro gerente. Ao excluir, seus dados pessoais são anonimizados, conforme a [Política de Privacidade](./POLITICA_DE_PRIVACIDADE.md). A exclusão não pode ser desfeita.
- Os registros de pedidos e as avaliações publicadas são mantidos após a exclusão, vinculados apenas ao registro anonimizado, conforme a Política de Privacidade. Para remover suas avaliações, apague-as antes de excluir a conta.

---

## 10. Conteúdo e propriedade intelectual

- As informações dos livros (título, autores, ano, número de páginas e ISBN) são obtidas pela **Google Books API**, e as capas pela **Open Library** ou por um link informado pela equipe. Esse conteúdo pertence aos seus respectivos titulares e é exibido apenas para identificar os exemplares do acervo.
- O conteúdo das avaliações é de responsabilidade de quem as publica. Ao publicar uma avaliação, você autoriza a sua exibição no sistema enquanto ela estiver publicada.
- O código, o layout e a identidade visual da Biblioteca Pallanthir pertencem à equipe do projeto.

---

## 11. Disponibilidade e limitação de responsabilidade

- O sistema é oferecido **"no estado em que se encontra"**, sem garantia de funcionamento ininterrupto.
- A hospedagem utiliza planos gratuitos. Após um período sem uso, o servidor pode demorar alguns segundos para responder ao primeiro acesso.
- Os e-mails com códigos de acesso podem atrasar ou ser direcionados à caixa de spam.
- O processamento dos pagamentos depende do Mercado Pago. Instabilidades no Mercado Pago podem atrasar a confirmação de um pedido.
- A equipe do projeto não se responsabiliza por indisponibilidades, perda de reservas causada por falhas técnicas ou danos decorrentes do uso indevido da conta pelo próprio usuário ou por terceiros a quem ele tenha fornecido seus dados de acesso. Essa limitação não afasta os direitos garantidos ao consumidor pelo Código de Defesa do Consumidor.

---

## 12. Alterações destes Termos

Estes Termos podem ser atualizados a qualquer momento. A data da última atualização aparece no início do documento. Quando estes Termos ou a Política de Privacidade mudarem, o sistema pedirá que você leia e aceite a nova versão no próximo acesso. Até o novo aceite, não é possível reservar livros nem finalizar compras.

---

## 13. Contato e legislação

- **Contato:** [bibliotecapallanthir@gmail.com](mailto:bibliotecapallanthir@gmail.com)
- Estes Termos são regidos pelas leis da República Federativa do Brasil, incluindo o Marco Civil da Internet (Lei nº 12.965/2014), a Lei Geral de Proteção de Dados (Lei nº 13.709/2018) e o Código de Defesa do Consumidor (Lei nº 8.078/1990).
