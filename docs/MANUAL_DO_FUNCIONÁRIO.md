# Manual do Funcionário — Biblioteca Pallanthir

**Versão do sistema:** 4.0
**Última atualização:** 23/11/2026

Este manual explica, passo a passo, como a **equipe da biblioteca** (funcionários e gerentes) usa a **Biblioteca Pallanthir**, disponível em [pallanthir.vercel.app](https://pallanthir.vercel.app): cuidar do acervo, registrar retiradas, devoluções e compras, aplicar multas e acompanhar os números do sistema.

O passo a passo do estudante está no [Manual do Usuário](./MANUAL_DO_USUARIO.md). As regras completas estão nos [Termos de Uso](./TERMOS_DE_USO.md), na [Política de Privacidade](./POLITICA_DE_PRIVACIDADE.md) e na [Política de Uso de Reservas](./POLITICA_DE_RESERVAS.md).

---

## 1. Perfis da equipe

| Perfil | Como chega a esse perfil | O que pode fazer |
|---|---|---|
| **Funcionário** | Cadastro no site com o código de acesso da biblioteca | Cuidar do acervo, registrar retiradas, devoluções e compras retiradas, aplicar e dispensar multas e ver a dashboard |
| **Gerente** | Promovido por outro gerente | Tudo o que o funcionário faz, mais Equipe, Documentos, Vendas e Logs |

Menu do **funcionário:** Início, Matérias, Ranking, Gestão e Dashboard.
Menu do **gerente:** Início, Matérias, Ranking, Gestão, Equipe, Dashboard, Vendas, Documentos e Logs.

### Prazos que a equipe acompanha

| O quê | Prazo |
|---|---|
| Retirada de um livro reservado | 3 dias depois da reserva (depois disso, a reserva é cancelada sozinha) |
| Devolução de um livro reservado | 7 dias depois da retirada |
| Pagamento de um pedido | 30 minutos (depois disso, o pedido expira) |
| Retirada de uma compra aprovada | 15 dias (depois disso, o reembolso é automático) |
| Link de pagamento de uma multa | 60 minutos |

---

## 2. Acesso ao sistema

### 2.1 Criar a conta de funcionário
1. Abra o site e vá até a tela de cadastro.
2. Informe nome, e-mail e senha. A senha precisa ter **no mínimo 8 caracteres**, com letra maiúscula, letra minúscula, número e um caractere especial (`@ # $ % ^ & + = !`).
3. Escolha o tipo de conta **Funcionário** e digite o **código de acesso** fornecido pela administração da biblioteca.
4. Marque **"Li e aceito os Termos de Uso e a Política de Privacidade"**.
5. Clique em **"Criar conta"**.

O código de acesso **não deve ser compartilhado** com estudantes. Não existe cadastro de Gerente: um funcionário só vira gerente quando outro gerente o promove (seção 6.1).

### 2.2 Entrar no site
1. Informe e-mail e senha.
2. Digite o **código de 6 dígitos** enviado por e-mail. Ele vale **10 minutos**. Se não chegar, clique em **"Reenviar código"** e confira a caixa de spam.

Regras de segurança:
- Depois de **3 senhas erradas**, a conta fica bloqueada por **15 minutos**.
- Um código deixa de valer depois de **5 tentativas erradas**.
- A sessão termina sozinha depois de **15 minutos sem atividade**. Ao sair do balcão, clique em **"Sair da conta"**.

---

## 3. Acervo

### 3.1 Cadastrar um livro
1. Abra o cadastro de livro e digite o **título**. Autores, ano, número de páginas e ISBN vêm da **Google Books API**, e a capa vem da **Open Library**.
2. Defina a **matéria**, o **preço**, o **estoque para reserva** e o **estoque à venda**. Valores negativos não são aceitos.
3. Salve.

Livros novos começam com **1 exemplar para reserva** e **nenhum à venda**. Um livro só aparece à venda quando tem estoque de venda e preço maior que zero.

Se o livro **já está no acervo**, o cadastro é bloqueado. Para ter mais exemplares, edite o estoque do livro que já existe.

### 3.2 Editar um livro
- Altere qualquer dado, os **dois estoques**, o **ISBN-10** e o **ISBN-13**.
- Se a capa não foi encontrada, informe um **link da capa**: ele precisa ser `https`, ter até 255 caracteres e apontar para uma imagem que existe. Sem capa, o site mostra uma **capa genérica** com o título do livro.

### 3.3 Excluir um livro
Ao excluir um livro com reservas em aberto, o site pede confirmação. O livro **some do catálogo e dos favoritos**, mas continua no histórico das reservas.

---

## 4. Gestão

Na página **Gestão** você registra tudo o que acontece no balcão. Busque os estudantes pelo **nome**: fora da página Minha conta, os e-mails aparecem **parcialmente escondidos** (por exemplo, `fe•••@gmail.com`).

| Aba | O que fazer |
|---|---|
| **Reservas** | Registrar a **entrega** e a **devolução** dos livros reservados, com os prazos de cada reserva |
| **Compras para retirar** | Registrar a retirada das compras pagas |
| **Multas** | Ver as multas pendentes e dispensar uma multa registrada por engano |

### 4.1 Entregar um livro reservado
1. O estudante chega ao balcão para retirar o livro.
2. Confira o estado do exemplar **com o estudante**. Se houver algum problema, anote antes de entregar, para que ele não seja responsabilizado.
3. Na aba **Reservas**, encontre a reserva e registre a **entrega**. O status passa de **Reservado** para **Entregue**, e o prazo de devolução de **7 dias** começa a contar.

Reservas não retiradas em **3 dias** são canceladas sozinhas, e o exemplar volta ao estoque.

### 4.2 Receber uma devolução
1. Confira o estado do livro devolvido.
2. Na aba **Reservas**, registre a **devolução**. O status passa de **Entregue** para **Devolvido**, e o exemplar volta ao estoque para reserva.
3. Se o livro estiver danificado, siga a seção 4.4.

Enquanto o estudante tiver uma devolução atrasada, ele não consegue fazer novas reservas. O bloqueio termina assim que a devolução é registrada.

### 4.3 Entregar uma compra
1. O estudante chega com uma compra **Aprovada**. O prazo para retirar é de **15 dias** depois do pagamento.
2. Na aba **Compras para retirar**, busque pelo nome do estudante ou pelo número do pedido.
3. Entregue os livros e registre a retirada. O pedido passa para **Entregue**.

Compras não retiradas em 15 dias são **reembolsadas automaticamente**, e os exemplares voltam ao estoque. Enquanto a compra não é retirada, o próprio estudante também pode pedir o reembolso pelo site.

### 4.4 Devolução com dano e multa
1. Ao registrar a devolução, marque o livro como **danificado**.
2. A multa usa o **valor do exemplar**. Se precisar, informe outro valor.
3. Escreva uma **observação** de até 300 caracteres explicando o dano.
4. Confirme. O livro **sai de circulação**, e a multa aparece para o estudante em **Minha conta** e em um aviso no topo do site.

Contam como dano, por exemplo: páginas rasgadas ou faltando, capa ou lombada danificadas, rabiscos, grifos ou manchas, danos por água, comida, calor ou umidade, e perda do exemplar. O **desgaste natural** do uso cuidadoso **não** é dano.

Enquanto a multa estiver pendente, o estudante **não consegue reservar, comprar nem excluir a conta**. Ele paga pelo Mercado Pago no próprio site.

### 4.5 Dispensar uma multa
Se uma multa foi registrada por engano, abra a aba **Multas**, encontre a multa pendente e **dispense-a**. A ação fica registrada nos logs.

---

## 5. Dashboard

A página **Dashboard** mostra os números gerais da biblioteca, sem identificar nenhum estudante:
- **Acervo:** títulos ativos, exemplares para reserva, exemplares à venda e títulos esgotados para reserva;
- **Reservas:** quantidade de reservadas, entregues, devolvidas e canceladas;
- **Matérias mais reservadas** e **matérias mais compradas** (as 5 primeiras).

Clique em **Atualizar** para recarregar os números.

---

## 6. Área do gerente

Esta seção vale só para contas do tipo **Gerente**.

### 6.1 Equipe
- **Promover** um funcionário a gerente ou **rebaixar** um gerente a funcionário.
- **Excluir** a conta de um funcionário. Os dados dele são anonimizados.
- O sistema **nunca fica sem gerente**: o último gerente ativo não pode ser rebaixado nem excluído, nem excluir a própria conta.
- Você não altera o próprio perfil nem exclui a própria conta pela página Equipe.

O **primeiro gerente** do sistema é definido direto no banco de dados, conforme o [README](./README.md#41-primeiro-gerente).

### 6.2 Documentos
1. Escolha os **Termos de Uso**, a **Política de Privacidade** ou a **Política de Reservas**.
2. Edite o texto em Markdown, conferindo na **pré-visualização**.
3. Salve. A versão anterior vai para o **histórico**, com a data e o nome de quem alterou, e pode ser aberta depois.

**Atenção:** mudar os Termos de Uso ou a Política de Privacidade faz **todos os usuários** aceitarem a nova versão ao entrar. Até lá, eles não conseguem reservar nem comprar. Mudar a Política de Reservas exige novo aceite na próxima reserva.

### 6.3 Vendas
Escolha um **período** para ver:
- **Resumo:** faturamento, pedidos pagos, ticket médio, exemplares vendidos, compras aguardando retirada, compras entregues e reembolsos;
- **Pedidos por status**;
- **Livros e matérias mais vendidos**;
- **Vendas por mês**;
- **Multas:** recebidas e pendentes.

Os números de vendas contam os pedidos **aprovados e entregues**.

### 6.4 Logs de auditoria
Os logs mostram as ações feitas no sistema (logins, reservas, pedidos, pagamentos, multas, alterações de livros e documentos, alterações na equipe e acessos negados), da mais recente para a mais antiga, em páginas de **50 registros**.

- Filtre por **usuário**, **ação** e **período**, ou busque por nome, ação, palavra da descrição ou e-mail completo.
- Os e-mails aparecem **escondidos**. Para ver um e-mail completo, clique no **ícone de olho**. Cada visualização fica registrada nos logs com o seu nome.
- Os logs são **apagados automaticamente 12 meses** depois de cada registro.

---

## 7. Boas práticas e perguntas frequentes

### Boas práticas no balcão
- Confira o estado do livro **na entrega e na devolução**, sempre junto com o estudante.
- Registre a entrega e a devolução **na hora**, para os prazos e os bloqueios ficarem corretos.
- Nunca compartilhe o código de acesso de funcionário nem a sua senha.
- Saia da conta ao deixar o computador do balcão.

### Perguntas frequentes

| Situação | O que fazer |
|---|---|
| O livro que quero cadastrar foi bloqueado | Ele já está no acervo: edite o estoque do livro existente |
| A capa não apareceu | Informe um link `https` da capa na edição do livro, ou deixe a capa genérica |
| Não acho o e-mail do estudante | Os e-mails ficam escondidos: busque pelo nome do estudante ou pelo número do pedido |
| Registrei uma multa por engano | Dispense a multa na aba Multas |
| O estudante diz que pagou, mas a multa ou o pedido continua pendente | A confirmação do Mercado Pago pode levar alguns instantes; peça para ele reabrir Minha conta ou Meus pedidos |
| Não vejo Equipe, Vendas, Documentos ou Logs no menu | Essas páginas são só do gerente |
| O site demorou para abrir | O servidor usa plano gratuito e "acorda" no primeiro acesso; pode levar cerca de um minuto |

### Contato
- **E-mail:** [bibliotecapallanthir@gmail.com](mailto:bibliotecapallanthir@gmail.com)
- Problemas de segurança devem ser relatados como explica a [Política de Segurança](./SECURITY.md).
