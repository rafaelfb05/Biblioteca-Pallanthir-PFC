# Manual do Usuário — Biblioteca Pallanthir

**Versão do sistema:** 4.0
**Última atualização:** 23/11/2026

Este manual explica, passo a passo, como os **estudantes** usam a **Biblioteca Pallanthir**, disponível em [pallanthir.vercel.app](https://pallanthir.vercel.app): criar a conta, encontrar e avaliar livros, reservar, comprar, pagar multas e pedir avisos de disponibilidade.

As regras completas estão nos [Termos de Uso](./TERMOS_DE_USO.md), na [Política de Privacidade](./POLITICA_DE_PRIVACIDADE.md) e na [Política de Uso de Reservas](./POLITICA_DE_RESERVAS.md).

---

## 1. Sobre o sistema

A Biblioteca Pallanthir é um site de biblioteca para estudantes, onde você consulta o acervo, avalia, favorita, reserva e compra **livros físicos**.

O sistema **não oferece livros digitais**: as reservas separam um exemplar para retirada no balcão da biblioteca, e as compras são de exemplares do acervo, pagas pelo **Mercado Pago**.

### Prazos em resumo

| O quê | Prazo |
|---|---|
| Código de login (2FA) | 10 minutos |
| Código de recuperação de senha ou de troca de e-mail | 15 minutos |
| Sessão sem atividade | 15 minutos |
| Retirar um livro reservado | 3 dias depois da reserva |
| Devolver um livro reservado | 7 dias depois da retirada |
| Pagar um pedido | 30 minutos |
| Retirar uma compra aprovada | 15 dias |
| Pagar o link de uma multa | 60 minutos |
| Desistir de uma compra já retirada | 7 dias |

---

## 2. Primeiros passos

Para usar o site como estudante, você cria uma conta e entra com a senha mais um código enviado por e-mail.

### 2.1 Criar uma conta
1. Abra o site e vá até a tela de cadastro.
2. Informe nome, e-mail e senha. A senha precisa ter **no mínimo 8 caracteres**, com letra maiúscula, letra minúscula, número e um caractere especial (`@ # $ % ^ & + = !`).
3. Escolha o tipo de conta: **Estudante**.
4. Marque **"Li e aceito os Termos de Uso e a Política de Privacidade"**. Clique no nome de cada documento para lê-lo antes de aceitar.
5. Clique em **"Criar conta"**.

### 2.2 Entrar no site
1. Informe e-mail e senha.
2. Abra o e-mail que chegou com um **código de 6 dígitos**. O link do e-mail abre uma página que copia o código com um clique.
3. Digite o código no site. Ele vale **10 minutos**. Se não chegar, clique em **"Reenviar código"** e confira a caixa de spam.

Regras de segurança do login:
- Depois de **3 senhas erradas**, a conta fica bloqueada por **15 minutos**.
- Um código deixa de valer depois de **5 tentativas erradas**; peça um novo.
- A sessão termina sozinha depois de **15 minutos sem atividade**, e o site avisa quando isso acontece. Para sair antes, clique em **"Sair da conta"**.

### 2.3 Esqueci minha senha
1. Na tela de login, clique em **"Esqueci minha senha"** e informe o seu e-mail.
2. Digite o código que chegar por e-mail. Ele vale **15 minutos**, e um novo código só pode ser pedido depois de **1 minuto**.
3. Crie a nova senha, seguindo as mesmas regras do cadastro.

Por segurança, o site responde "Se o e-mail estiver cadastrado…" mesmo quando o e-mail não existe.

### 2.4 Minha conta
Na página **Minha conta** você:
- edita o seu nome;
- troca o e-mail: um código vai para o **novo endereço**, a troca só vale depois que você confirma, e em seguida é preciso entrar de novo;
- troca a senha;
- lê os Termos de Uso, a Política de Privacidade e a Política de Reservas em **Documentos**;
- vê as suas multas pendentes, se houver;
- exclui a conta (veja a [seção 7](#7-perguntas-frequentes)).

Quando os Termos de Uso ou a Política de Privacidade mudam, aparece uma janela pedindo um **novo aceite** ao entrar. Abra os dois documentos e aceite: até lá, não é possível reservar nem finalizar compras.

---

## 3. Encontrar e avaliar livros

Qualquer pessoa, mesmo sem conta, navega pelo acervo; avaliar e favoritar exige entrar como estudante.

### 3.1 Acervo, busca e matérias
- A página **Início** mostra os livros ativos em páginas de **18 livros**.
- Use a busca para procurar pelo **título**.
- Em **Matérias**, filtre por área (TI, Direito, Medicina, Odontologia, Veterinária, Física, Química, Arquitetura e Biologia), com a quantidade de livros de cada uma.
- Em **Ranking**, veja os **10 livros mais bem avaliados**, contando só livros com pelo menos 3 avaliações. O ranking também pode ser filtrado por matéria.
- O menu abre pelo botão no cabeçalho, em formato de gaveta.

### 3.2 Página do livro
Ao abrir um livro você vê a capa (clique para ampliar), o ISBN, o preço, o **estoque para reserva**, o **estoque à venda**, a média das notas e os comentários.

Os dois estoques são separados: um livro pode estar disponível para reserva e esgotado para venda, ou o contrário. O card do livro mostra **"Reservado"** quando você já tem uma reserva em aberto dele.

### 3.3 Avaliar um livro
1. Na página do livro, escolha uma nota de **1 a 5 estrelas**.
2. Se quiser, escreva um comentário de até **500 caracteres**.
3. Clique em **"Enviar avaliação"**.

Cada estudante tem **uma avaliação por livro**: avaliar de novo substitui a anterior, e você pode removê-la a qualquer momento. As avaliações são **públicas** e mostram o seu nome, então não escreva dados pessoais no comentário.

### 3.4 Favoritos
Clique em **"Adicionar aos favoritos"** no card ou na página do livro para salvá-lo; para tirar, clique em **"Remover dos favoritos"**. Os livros salvos ficam em **Meus favoritos**.

---

## 4. Reservar livros

A reserva separa um exemplar para você por **3 dias**; depois de retirado, o livro deve voltar em **7 dias**.

### 4.1 Como reservar
1. Abra a página de um livro com estoque para reserva.
2. Clique em **"Reservar"**.
3. Leia a [Política de Uso de Reservas](./POLITICA_DE_RESERVAS.md) e aceite. O site guarda a data, a hora e a versão que você aceitou.
4. Vá ao balcão da biblioteca em até **3 dias** para retirar o livro. Um funcionário registra a retirada.
5. Devolva o livro no balcão em até **7 dias**. Um funcionário registra a devolução.

Os dois prazos aparecem em **Minhas reservas**, que lista as reservas da mais recente para a mais antiga, com a capa de cada livro.

### 4.2 Status da reserva

| Status | O que significa |
|---|---|
| **Reservado** | O exemplar está separado, esperando a retirada |
| **Entregue** | Você retirou o livro e ele está com você |
| **Devolvido** | O livro voltou para a biblioteca |
| **Cancelado** | A reserva foi cancelada por você ou automaticamente, após 3 dias sem retirada |

### 4.3 Regras importantes
- **Limite:** uma reserva em aberto por livro e até **3 reservas em aberto** ao mesmo tempo, contando as reservadas e as que estão com você.
- **Cancelar:** só enquanto a reserva está como **Reservado**; o exemplar volta ao estoque.
- **Atraso:** com uma devolução atrasada, você não consegue fazer novas reservas até o livro ser devolvido.
- **Estado do livro:** se notar algum problema na retirada, avise a equipe **antes** de sair da biblioteca, para não ser responsabilizado.
- **Dano ou perda:** gera uma multa no valor do exemplar (veja a [seção 6](#6-multas-e-avisos-de-disponibilidade)).

---

## 5. Comprar livros

Você paga pelo Mercado Pago e retira os livros no balcão em até **15 dias** depois da aprovação do pagamento.

### 5.1 Como comprar
1. Clique em **"Adicionar ao carrinho"** no card ou na página de um livro com estoque à venda e preço.
2. Abra **Meu carrinho** e ajuste as quantidades. A quantidade não passa do estoque à venda.
3. Clique em **"Pagar com Mercado Pago"**. O pedido é criado e os exemplares ficam **separados por 30 minutos**.
4. Pague na página do Mercado Pago. O site **nunca recebe os dados do seu cartão**.
5. Ao voltar, a página **Meus pedidos** mostra o resultado e atualiza o status sozinha.
6. Com o pedido **Aprovado**, retire os livros no balcão em até **15 dias**. Um funcionário registra a retirada.

Enquanto um pedido espera pagamento, o carrinho fica bloqueado e não é possível criar outro pedido. Para pagar depois, use o botão de pagar no carrinho ou em **Meus pedidos**; para desistir, cancele o pedido.

### 5.2 Status do pedido

| Status | O que significa |
|---|---|
| **Aguardando pagamento** | Pedido criado, exemplares separados por 30 minutos |
| **Aprovado** | Pagamento confirmado; os livros esperam a retirada |
| **Entregue** | Você retirou os livros |
| **Cancelado** | Você cancelou antes de pagar; nada foi cobrado |
| **Expirado** | O pagamento não foi confirmado em 30 minutos; nada foi cobrado |
| **Reembolsado** | O valor voltou pelo Mercado Pago |

### 5.3 Cancelamento e reembolso
- **Antes de pagar:** cancele o pedido em **Meus pedidos**. Os exemplares voltam ao estoque.
- **Pago e ainda não retirado:** peça o reembolso em **Meus pedidos**. O valor volta no mesmo meio de pagamento, nos prazos do Mercado Pago.
- **Não retirou em 15 dias:** o reembolso é **automático**.
- **Já retirou:** você pode desistir em até **7 dias** (art. 49 do Código de Defesa do Consumidor). Envie o pedido com o número da compra para o e-mail da [seção 7](#7-perguntas-frequentes).

Não é possível comprar com uma **multa pendente** ou sem ter aceitado a versão atual dos termos.

---

## 6. Multas e avisos de disponibilidade

### 6.1 Multa por dano
Se um livro reservado voltar danificado ou for perdido, a equipe aplica uma **multa no valor do exemplar** (ou no valor que informar), e o livro sai de circulação.

1. A multa aparece em **Minha conta** e em um aviso no topo do site.
2. Clique para pagar: você vai para o Mercado Pago. O link de pagamento vale **60 minutos**; se expirar, gere outro.
3. Depois que o Mercado Pago confirma, a multa sai da lista.

Enquanto houver multa pendente, você **não consegue reservar, comprar nem excluir a conta**. Se a multa foi registrada por engano, procure a equipe da biblioteca, que pode cancelá-la.

O desgaste natural do uso cuidadoso **não** conta como dano. Contam, por exemplo, páginas rasgadas ou faltando, capa danificada, rabiscos ou grifos, manchas de água ou comida, e perda do exemplar.

### 6.2 Aviso de disponibilidade
Quando um livro está esgotado, aparece um **sino** ao lado do estoque esgotado (reserva ou venda).

1. Clique no sino e confirme que quer ser avisado.
2. Quando o estoque voltar, você recebe **um** e-mail com a capa do livro e um link para a página dele.
3. Para cancelar, clique no sino de novo.

O aviso **não reserva nem separa** o exemplar: reserve ou compre assim que receber o e-mail.

---

## 7. Perguntas frequentes

| Situação | O que fazer |
|---|---|
| O código de login não chegou | Confira a caixa de spam e clique em **"Reenviar código"**. Os e-mails podem atrasar alguns minutos |
| Minha conta está bloqueada | Foram 3 senhas erradas: espere 15 minutos ou recupere a senha |
| O código diz "inválido ou expirado" | Ele passou do prazo ou teve 5 tentativas erradas: peça um novo código |
| Não consigo reservar | Veja se o livro tem estoque para reserva, se você já tem 3 reservas em aberto, se há devolução atrasada, multa pendente ou um aceite de termos pendente |
| Não consigo comprar | Veja se há um pedido aguardando pagamento, multa pendente ou aceite de termos pendente |
| Paguei e o pedido não foi aprovado | A confirmação do Mercado Pago pode levar alguns instantes; reabra **Meus pedidos** |
| O site demorou para abrir | O servidor usa plano gratuito e "acorda" no primeiro acesso; pode levar cerca de um minuto |
| Fui desconectado | A sessão termina após 15 minutos sem atividade; entre de novo |

### Excluir a conta
Em **Minha conta → Excluir minha conta**. Só é possível sem pendências: nenhuma reserva **Reservado** ou **Entregue**, nenhum pedido aguardando pagamento ou retirada e nenhuma multa pendente.

Seu nome vira "Usuário removido", o e-mail é anonimizado, e a exclusão **não pode ser desfeita**. Suas avaliações publicadas continuam visíveis com esse nome; apague-as antes, se preferir.

### Contato
- **E-mail:** [bibliotecapallanthir@gmail.com](mailto:bibliotecapallanthir@gmail.com)
- Para pedidos sobre compras, informe o número do pedido.
