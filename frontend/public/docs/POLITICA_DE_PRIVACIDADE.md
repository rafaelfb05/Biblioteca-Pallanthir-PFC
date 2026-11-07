# Política de Privacidade — Biblioteca Pallanthir

**Última atualização:** 07/11/2026

Esta Política de Privacidade explica quais dados pessoais a **Biblioteca Pallanthir** coleta, para que eles são usados, com quem são compartilhados, por quanto tempo são mantidos e quais são os seus direitos, em conformidade com a **Lei Geral de Proteção de Dados (LGPD — Lei nº 13.709/2018)**.

Ela complementa os [Termos de Uso](./TERMOS_DE_USO.md).

---

## 1. Quem é o responsável pelos dados

O controlador dos dados é a **equipe do projeto Biblioteca Pallanthir**, desenvolvido como um sistema de biblioteca virtual.

- **Contato para assuntos de privacidade:** [bibliotecapallanthir@gmail.com](mailto:bibliotecapallanthir@gmail.com)

---

## 2. Quais dados coletamos

### 2.1 Dados que você informa

| Dado | Quando é coletado | Observação |
|---|---|---|
| Nome | Cadastro e edição da conta | — |
| E-mail | Cadastro, login e edição da conta | Usado como identificador da conta e para envio de códigos. |
| Senha | Cadastro e redefinição de senha | Armazenada **somente como hash BCrypt**. A senha original não é guardada nem pode ser consultada. |
| Tipo de conta | Cadastro | Estudante ou Funcionário. |
| Código de acesso de funcionário | Cadastro como funcionário | Apenas conferido no momento do cadastro, **não é armazenado**. |
| Aceite dos Termos de Uso e desta Política | Cadastro | Guardamos a data e a hora em que você marcou o aceite, como comprovação do consentimento. |

### 2.2 Dados gerados pelo uso do sistema

| Dado | Finalidade |
|---|---|
| Livros favoritados | Exibir a sua lista de favoritos. |
| Reservas (livro, data e status) | Controlar as reservas e o estoque. |
| Avaliações (livro, nota, comentário e data) | Exibir a avaliação na página do livro e calcular a média e o ranking. **As avaliações são públicas e exibem o seu nome** (ver seção 2.4). |
| Carrinho (livros e quantidades) | Guardar os livros que você pretende comprar. |
| Pedidos (livros, quantidades, preços, total, datas e status) | Controlar as compras, o estoque à venda e a retirada dos livros. |
| Identificador do pedido no Mercado Pago e link de pagamento | Permitir retomar o pagamento e confirmar o status junto ao Mercado Pago. |
| Código de verificação em duas etapas e sua validade | Confirmar o login. É apagado após o uso e expira em 10 minutos. |
| Código de recuperação de senha e sua validade | Permitir a redefinição da senha. É apagado após o uso e expira em 15 minutos. |
| Tentativas de login com senha incorreta e horário de bloqueio | Proteger a conta contra tentativas repetidas de acesso. |
| Logs de auditoria | Segurança e rastreabilidade (ver seção 2.3). |

### 2.3 Logs de auditoria

O sistema registra ações relevantes, como logins (com sucesso ou falha), verificação em duas etapas, cadastros, atualizações e exclusões de conta, favoritos, reservas, cancelamentos, entregas, devoluções, avaliações, criação de pedidos, pagamentos aprovados, pedidos expirados, alterações no acervo e tentativas de acesso negado.

Cada registro contém: **identificador do usuário (quando existir), e-mail utilizado, tipo da ação, descrição da ação e data/hora**. Os logs só podem ser consultados por contas do tipo **Funcionário**.

A **dashboard** dos funcionários mostra apenas números gerais (quantidade de livros, de reservas por status e de matérias mais reservadas e compradas), sem identificar nenhum usuário.

### 2.4 Avaliações públicas

Ao avaliar um livro, a nota, o comentário, a data e o **nome cadastrado na sua conta** passam a ser exibidos na página do livro para qualquer pessoa, inclusive visitantes sem conta. Não escreva dados pessoais nos comentários. Você pode remover a sua avaliação a qualquer momento, e alterar o seu nome em **Minha conta** também altera o nome exibido nas avaliações.

### 2.5 Pagamentos

O pagamento das compras é feito **diretamente na página do Mercado Pago**. Os dados que você informa ali, como número do cartão, código de segurança, CPF e e-mail do pagador, são tratados pelo **Mercado Pago, conforme a política de privacidade dele**, e **não são recebidos nem armazenados** pela Biblioteca Pallanthir.

Do pagamento, o sistema guarda apenas o identificador do pedido no Mercado Pago, o link de pagamento e o status (aguardando pagamento, aprovado ou expirado).

### 2.6 Dados armazenados no seu navegador

O sistema **não utiliza cookies**. Ele guarda no `localStorage` do seu navegador:

- `pallanthir:usuario` — dados da sessão (id, nome, e-mail, tipo de conta e o token de acesso);
- `pallanthir:ultimaAtividade` — horário da sua última atividade, usado para encerrar a sessão após 15 minutos de inatividade.

Esses dados são apagados ao sair da conta, ao excluir a conta ou quando a sessão expira.

O carrinho não fica no navegador: ele é guardado no servidor, vinculado à sua conta.

### 2.7 Dados que não coletamos

- Não coletamos telefone nem endereço.
- Não recebemos nem armazenamos dados de cartão, dados bancários ou o CPF informado no pagamento; esses dados ficam com o Mercado Pago.
- Não utilizamos ferramentas de rastreamento, anúncios ou análise de comportamento de terceiros.

---

## 3. Para que usamos os dados e com qual base legal

| Finalidade | Base legal (LGPD, art. 7º) |
|---|---|
| Criar e manter a sua conta, permitir login, favoritos e reservas | Execução de contrato ou de procedimentos relacionados, a pedido do titular (inciso V) |
| Publicar as suas avaliações, a seu pedido | Execução de contrato ou de procedimentos relacionados, a pedido do titular (inciso V) |
| Manter o carrinho, processar compras e confirmar pagamentos com o Mercado Pago | Execução de contrato (inciso V) |
| Guardar o histórico de pedidos para comprovar as compras e atender a pedidos de arrependimento ou reembolso | Execução de contrato (inciso V) e exercício regular de direitos (inciso VI) |
| Enviar códigos de verificação e de recuperação de senha por e-mail | Execução de contrato (inciso V) |
| Bloqueio por tentativas, verificação em duas etapas, encerramento por inatividade e logs de auditoria | Legítimo interesse, para garantir a segurança do sistema e das contas (inciso IX) |
| Atender a solicitações de autoridades | Cumprimento de obrigação legal ou regulatória (inciso II) |

Os dados **não são vendidos** nem usados para publicidade.

---

## 4. Com quem compartilhamos os dados

Para funcionar, o sistema utiliza serviços de terceiros que atuam como **operadores**, tratando os dados apenas para prestar o serviço contratado:

| Serviço | Uso | Dados envolvidos |
|---|---|---|
| **Supabase** (PostgreSQL) | Banco de dados principal | Conta, favoritos, reservas, avaliações, carrinho, pedidos, códigos temporários |
| **MongoDB Atlas** | Armazenamento dos logs de auditoria | Registros de log (seção 2.3) |
| **Render** | Hospedagem do servidor (back-end) | Todos os dados em trânsito pelo servidor |
| **Vercel** | Hospedagem do site (front-end) | Dados técnicos de acesso ao site (ex.: endereço IP) |
| **Brevo** | Envio dos e-mails com códigos | Nome do sistema, seu e-mail e o conteúdo da mensagem |
| **Google Books API** | Obtenção das informações dos livros | **Nenhum dado pessoal** é enviado |
| **Mercado Pago** | Processamento dos pagamentos | O sistema envia o número do pedido, os títulos, as quantidades e os preços dos livros. **Seu nome e seu e-mail não são enviados.** Os dados de pagamento você informa diretamente ao Mercado Pago (seção 2.5). |

O Mercado Pago trata os dados de pagamento como controlador independente, de acordo com os próprios termos e a própria política de privacidade.

Alguns desses serviços podem armazenar ou processar dados fora do Brasil. Nesses casos, a transferência ocorre para a execução do serviço solicitado por você (LGPD, art. 33, inciso IX) e com base nas garantias oferecidas por esses fornecedores.

Os dados também podem ser compartilhados com autoridades públicas quando houver obrigação legal ou ordem judicial.

---

## 5. Como protegemos os dados

- Senhas armazenadas apenas como hash **BCrypt**.
- Verificação em duas etapas por e-mail em todo login.
- Bloqueio de 15 minutos após 3 tentativas de login com senha incorreta.
- Autenticação por token **JWT** com validade curta, renovado apenas durante o uso ativo.
- Encerramento automático da sessão após 15 minutos de inatividade.
- Controle de permissões por tipo de conta: estudantes só acessam os próprios favoritos, reservas e dados; funções administrativas são restritas a funcionários.
- Registro de tentativas de acesso não autorizado nos logs de auditoria.
- Comunicação com o servidor feita por HTTPS.
- Pagamentos feitos na página do Mercado Pago, sem que os dados do cartão passem pelo sistema.
- Notificações de pagamento aceitas apenas com assinatura válida do Mercado Pago, e status confirmado diretamente na API do Mercado Pago.

Nenhum sistema é totalmente imune a incidentes. Caso ocorra um incidente de segurança que possa gerar risco relevante, os titulares afetados e a Autoridade Nacional de Proteção de Dados (ANPD) serão comunicados, conforme o art. 48 da LGPD.

---

## 6. Por quanto tempo mantemos os dados

| Dado | Tempo de retenção |
|---|---|
| Dados da conta | Enquanto a conta estiver ativa. |
| Código de verificação em duas etapas | Até ser usado ou expirar (10 minutos). |
| Código de recuperação de senha | Até ser usado ou expirar (15 minutos). |
| Reservas | Mantidas para controle do acervo, vinculadas à conta. Após a exclusão da conta, ficam associadas apenas ao registro anonimizado. |
| Avaliações | Enquanto estiverem publicadas. As avaliações removidas deixam de ser exibidas. Após a exclusão da conta, as avaliações publicadas continuam visíveis com o nome "Usuário removido". |
| Carrinho | Enquanto os itens estiverem no carrinho. Os itens saem quando o pedido é aprovado ou expira, ou quando você os remove. |
| Pedidos | Mantidos para comprovação das compras e atendimento a pedidos de arrependimento e reembolso. Após a exclusão da conta, ficam associados apenas ao registro anonimizado. |
| Logs de auditoria | Mantidos pelo tempo necessário para fins de segurança e rastreabilidade, enquanto o projeto estiver em funcionamento. |

---

## 7. Exclusão da conta

Você pode excluir sua conta em **Minha conta → Excluir minha conta**.

A exclusão só é permitida quando não há pendências com a biblioteca: se você tiver alguma reserva com status **Reservado** (livro ainda não retirado) ou **Entregue** (livro em sua posse e ainda não devolvido), será necessário cancelar a reserva ou devolver o livro antes. Essa restrição existe para garantir a devolução dos exemplares físicos ao acervo.

Ao excluir:

- seu nome é substituído por "Usuário removido";
- seu e-mail é substituído por um endereço anonimizado;
- sua senha é substituída por um valor aleatório, impossibilitando qualquer acesso;
- seus favoritos e códigos pendentes são apagados;
- a conta é desativada.

Os pedidos continuam registrados, associados apenas à conta anonimizada. As avaliações que estiverem publicadas continuam visíveis, mas passam a exibir o nome "Usuário removido". Se não quiser que elas permaneçam, remova-as antes de excluir a conta.

**Importante:** os logs de auditoria registrados **antes** da exclusão continuam armazenando o e-mail utilizado em cada ação, pelo período indicado na seção 6, para fins de segurança. Se desejar a remoção também desses registros, solicite pelo e-mail de contato.

---

## 8. Seus direitos

Conforme o art. 18 da LGPD, você pode solicitar a qualquer momento:

- confirmação de que tratamos seus dados;
- acesso aos seus dados;
- correção de dados incompletos, inexatos ou desatualizados (nome e e-mail podem ser alterados diretamente em **Minha conta**, e as avaliações podem ser refeitas ou removidas na página de cada livro);
- anonimização, bloqueio ou eliminação de dados desnecessários ou tratados em desconformidade com a LGPD;
- portabilidade dos dados;
- eliminação dos dados tratados com base no seu consentimento;
- informação sobre com quem seus dados são compartilhados;
- revisão de decisões tomadas unicamente com base em tratamento automatizado.

Para exercer seus direitos, envie um e-mail para [bibliotecapallanthir@gmail.com](mailto:bibliotecapallanthir@gmail.com) a partir do e-mail cadastrado na sua conta. A solicitação será respondida em até 15 dias.

Você também pode apresentar reclamação à **Autoridade Nacional de Proteção de Dados (ANPD)** pelo site [gov.br/anpd](https://www.gov.br/anpd).

---

## 9. Crianças e adolescentes

O sistema é destinado a estudantes. O cadastro de menores de 18 anos deve contar com a autorização de um dos pais ou responsável legal, conforme o art. 14 da LGPD.

---

## 10. Alterações desta Política

Esta Política pode ser atualizada para refletir mudanças no sistema ou na legislação. A data da última atualização aparece no início do documento. Alterações relevantes serão informadas no próprio sistema.
