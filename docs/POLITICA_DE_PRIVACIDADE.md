# Política de Privacidade — Biblioteca Pallanthir

**Última atualização:** 22/11/2026

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
| E-mail | Cadastro, login e edição da conta | Usado como identificador da conta, para envio de códigos e dos avisos de disponibilidade que você pedir. Ao trocar o e-mail, o novo endereço fica guardado como pendente até a confirmação do código. |
| Senha | Cadastro e redefinição de senha | Armazenada **somente como hash BCrypt**. A senha original não é guardada nem pode ser consultada. |
| Tipo de conta | Cadastro | Estudante ou Funcionário. Um funcionário pode ser promovido a Gerente por outro gerente. |
| Código de acesso de funcionário | Cadastro como funcionário | Apenas conferido no momento do cadastro, **não é armazenado**. |
| Aceite dos Termos de Uso e desta Política | Cadastro e a cada nova versão dos documentos | Guardamos a data, a hora e a versão dos documentos que você aceitou, como comprovação do consentimento. |
| Aceite da Política de Uso de Reservas | A cada reserva | Guardamos, junto com a reserva, a data, a hora e a versão da política aceita. |

### 2.2 Dados gerados pelo uso do sistema

| Dado | Finalidade |
|---|---|
| Livros favoritados | Exibir a sua lista de favoritos. |
| Reservas (livro, data, status e prazos de retirada e de devolução) | Controlar as reservas, os prazos e o estoque. |
| Avaliações (livro, nota, comentário e data) | Exibir a avaliação na página do livro e calcular a média e o ranking. **As avaliações são públicas e exibem o seu nome** (ver seção 2.5). |
| Carrinho (livros e quantidades) | Guardar os livros que você pretende comprar. |
| Pedidos (livros, quantidades, preços, total, datas, prazo de retirada, status e data do reembolso) | Controlar as compras, o estoque à venda, a retirada dos livros, os cancelamentos e os reembolsos. |
| Multas (livro, valor, observação do funcionário, status e datas) | Cobrar o valor de um livro devolvido com dano e bloquear novas reservas e compras enquanto a multa estiver pendente. |
| Avisos de disponibilidade (livro, tipo de estoque, status e data de envio) | Enviar o e-mail avisando que um livro esgotado voltou ao estoque. |
| Identificador do pedido ou da multa no Mercado Pago e link de pagamento | Permitir retomar o pagamento, confirmar o status e pedir reembolsos junto ao Mercado Pago. |
| Código de verificação em duas etapas e sua validade | Confirmar o login. É apagado após o uso e expira em 10 minutos. |
| Código de recuperação de senha e sua validade | Permitir a redefinição da senha. É apagado após o uso e expira em 15 minutos. |
| Código de troca de e-mail e sua validade | Confirmar que o novo e-mail é seu. É apagado após o uso e expira em 15 minutos. |
| Tentativas erradas de cada código | Invalidar o código depois de 5 tentativas erradas. |
| Tentativas de login com senha incorreta e horário de bloqueio | Proteger a conta contra tentativas repetidas de acesso. |
| Logs de auditoria | Segurança e rastreabilidade (ver seção 2.3). |

### 2.3 Logs de auditoria

O sistema registra ações relevantes, como logins (com sucesso ou falha), verificação em duas etapas, cadastros, atualizações e exclusões de conta, trocas de e-mail, aceites dos termos, favoritos, reservas, cancelamentos, entregas, devoluções, multas, avaliações, pedidos, pagamentos, reembolsos, avisos de disponibilidade, alterações no acervo e nos documentos, alterações na equipe e tentativas de acesso negado.

Cada registro contém: **identificador do usuário (quando existir), e-mail utilizado, tipo da ação, descrição da ação e data/hora**. Os logs só podem ser consultados por contas do tipo **Gerente**.

Na tela de logs, os e-mails aparecem **parcialmente escondidos** (por exemplo, `fe•••@gmail.com`). O gerente pode exibir o e-mail completo de um registro quando precisar, e **cada exibição fica registrada nos próprios logs**, com o nome de quem a fez.

A **dashboard** dos funcionários e a **dashboard de vendas** do gerente mostram apenas números gerais (livros, reservas por status, matérias e livros mais reservados e vendidos, faturamento e multas), sem identificar nenhum usuário.

### 2.4 Dados visíveis para a equipe da biblioteca

Para registrar retiradas, devoluções e multas, os funcionários e gerentes veem o **nome** dos estudantes e os livros reservados ou comprados. Fora da página **Minha conta**, que mostra os seus próprios dados, o e-mail aparece sempre **parcialmente escondido** nas telas da equipe. O endereço completo não é enviado ao navegador dessas telas.

### 2.5 Avaliações públicas

Ao avaliar um livro, a nota, o comentário, a data e o **nome cadastrado na sua conta** passam a ser exibidos na página do livro para qualquer pessoa, inclusive visitantes sem conta. Não escreva dados pessoais nos comentários. Você pode remover a sua avaliação a qualquer momento, e alterar o seu nome em **Minha conta** também altera o nome exibido nas avaliações.

### 2.6 Pagamentos

O pagamento das compras e das multas é feito **diretamente na página do Mercado Pago**. Os dados que você informa ali, como número do cartão, código de segurança, CPF e e-mail do pagador, são tratados pelo **Mercado Pago, conforme a política de privacidade dele**, e **não são recebidos nem armazenados** pela Biblioteca Pallanthir.

Do pagamento, o sistema guarda apenas o identificador do pedido ou da multa no Mercado Pago, o link de pagamento e o status. Os reembolsos são pedidos ao Mercado Pago pelo identificador do pedido, sem envolver os seus dados de pagamento.

### 2.7 Dados armazenados no seu navegador

O sistema **não utiliza cookies**. Ele guarda no `localStorage` do seu navegador:

- `pallanthir:usuario` — dados da sessão (id, nome, e-mail, tipo de conta e o token de acesso);
- `pallanthir:ultimaAtividade` — horário da sua última atividade, usado para encerrar a sessão após 15 minutos de inatividade.

Esses dados são apagados ao sair da conta, ao excluir a conta ou quando a sessão expira.

O carrinho não fica no navegador: ele é guardado no servidor, vinculado à sua conta.

### 2.8 Dados que não coletamos

- Não coletamos telefone nem endereço.
- Não recebemos nem armazenamos dados de cartão, dados bancários ou o CPF informado no pagamento; esses dados ficam com o Mercado Pago.
- Não utilizamos ferramentas de rastreamento, anúncios ou análise de comportamento de terceiros.

---

## 3. Para que usamos os dados e com qual base legal

| Finalidade | Base legal (LGPD, art. 7º) |
|---|---|
| Criar e manter a sua conta, permitir login, favoritos e reservas | Execução de contrato ou de procedimentos relacionados, a pedido do titular (inciso V) |
| Publicar as suas avaliações, a seu pedido | Execução de contrato ou de procedimentos relacionados, a pedido do titular (inciso V) |
| Manter o carrinho, processar compras, cancelamentos e reembolsos e confirmar pagamentos com o Mercado Pago | Execução de contrato (inciso V) |
| Controlar os prazos das reservas e cobrar multas por dano | Execução de contrato (inciso V) e exercício regular de direitos (inciso VI) |
| Enviar os avisos de disponibilidade que você pediu | Execução de contrato ou de procedimentos relacionados, a pedido do titular (inciso V) |
| Guardar o histórico de pedidos para comprovar as compras e atender a pedidos de arrependimento ou reembolso | Execução de contrato (inciso V) e exercício regular de direitos (inciso VI) |
| Enviar códigos de verificação, de recuperação de senha e de troca de e-mail | Execução de contrato (inciso V) |
| Bloqueio por tentativas, verificação em duas etapas, encerramento por inatividade e logs de auditoria | Legítimo interesse, para garantir a segurança do sistema e das contas (inciso IX) |
| Atender a solicitações de autoridades | Cumprimento de obrigação legal ou regulatória (inciso II) |

Os dados **não são vendidos** nem usados para publicidade.

---

## 4. Com quem compartilhamos os dados

Para funcionar, o sistema utiliza serviços de terceiros que atuam como **operadores**, tratando os dados apenas para prestar o serviço contratado:

| Serviço | Uso | Dados envolvidos |
|---|---|---|
| **Supabase** (PostgreSQL) | Banco de dados principal | Conta, favoritos, reservas, avaliações, carrinho, pedidos, multas, avisos de disponibilidade, códigos temporários |
| **MongoDB Atlas** | Armazenamento dos logs de auditoria | Registros de log (seção 2.3) |
| **Render** | Hospedagem do servidor (back-end) | Todos os dados em trânsito pelo servidor |
| **Vercel** | Hospedagem do site (front-end) | Dados técnicos de acesso ao site (ex.: endereço IP) |
| **Brevo** | Envio dos e-mails com códigos e dos avisos de disponibilidade | Seu nome, seu e-mail e o conteúdo da mensagem |
| **Google Books API** | Obtenção das informações dos livros | **Nenhum dado pessoal** é enviado |
| **Open Library** | Obtenção das capas dos livros | **Nenhum dado pessoal** é enviado |
| **Mercado Pago** | Processamento dos pagamentos e dos reembolsos | O sistema envia o número do pedido ou da multa, os títulos, as quantidades e os valores. **Seu nome e seu e-mail não são enviados.** Os dados de pagamento você informa diretamente ao Mercado Pago (seção 2.6). |

O Mercado Pago trata os dados de pagamento como controlador independente, de acordo com os próprios termos e a própria política de privacidade.

Alguns desses serviços podem armazenar ou processar dados fora do Brasil. Nesses casos, a transferência ocorre para a execução do serviço solicitado por você (LGPD, art. 33, inciso IX) e com base nas garantias oferecidas por esses fornecedores.

Os dados também podem ser compartilhados com autoridades públicas quando houver obrigação legal ou ordem judicial.

---

## 5. Como protegemos os dados

- Senhas armazenadas apenas como hash **BCrypt**.
- Verificação em duas etapas por e-mail em todo login.
- Códigos de verificação gerados de forma aleatória e segura, invalidados após 5 tentativas erradas.
- Troca de e-mail confirmada por código enviado ao novo endereço.
- Bloqueio de 15 minutos após 3 tentativas de login com senha incorreta.
- Autenticação por token **JWT** com validade curta, renovado apenas durante o uso ativo.
- Encerramento automático da sessão após 15 minutos de inatividade.
- Controle de permissões por tipo de conta: estudantes só acessam os próprios favoritos, reservas, pedidos, multas e dados; funções administrativas são restritas a funcionários, e os logs e a gestão da equipe, a gerentes.
- A cada requisição, o sistema confere se a conta continua ativa e com o mesmo e-mail e perfil, então um acesso deixa de valer assim que a conta é excluída ou alterada.
- E-mails parcialmente escondidos nas telas da equipe, com registro de cada exibição do e-mail completo nos logs.
- Acesso direto às tabelas do banco bloqueado por **Row Level Security**, permitindo o acesso apenas pelo servidor do sistema.
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
| Multas | Mantidas para comprovação da cobrança e do pagamento. Após a exclusão da conta, ficam associadas apenas ao registro anonimizado. |
| Avisos de disponibilidade | Até o envio ou o cancelamento. Os avisos pendentes são cancelados quando a conta é excluída. |
| Código de troca de e-mail | Até ser usado ou expirar (15 minutos). |
| Logs de auditoria | 12 meses a partir do registro. Depois desse prazo, os registros são apagados automaticamente. |

---

## 7. Exclusão da conta

Você pode excluir sua conta em **Minha conta → Excluir minha conta**.

A exclusão só é permitida quando não há pendências com a biblioteca: se você tiver alguma reserva com status **Reservado** (livro ainda não retirado) ou **Entregue** (livro em sua posse e ainda não devolvido), um pedido aguardando pagamento ou aguardando retirada, ou uma multa pendente, será necessário resolver a pendência antes. Essa restrição existe para garantir a devolução dos exemplares físicos ao acervo e a conclusão das compras.

Ao excluir:

- seu nome é substituído por "Usuário removido";
- seu e-mail é substituído por um endereço anonimizado;
- sua senha é substituída por um valor aleatório, impossibilitando qualquer acesso;
- seus favoritos e códigos pendentes são apagados, e seus avisos de disponibilidade são cancelados;
- a conta é desativada.

Os pedidos continuam registrados, associados apenas à conta anonimizada. As avaliações que estiverem publicadas continuam visíveis, mas passam a exibir o nome "Usuário removido". Se não quiser que elas permaneçam, remova-as antes de excluir a conta.

**Importante:** os logs de auditoria registrados **antes** da exclusão continuam armazenando o e-mail utilizado em cada ação, por até 12 meses a partir de cada registro (seção 6), para fins de segurança, e depois são apagados automaticamente. Se desejar a remoção também desses registros, solicite pelo e-mail de contato.

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

Esta Política pode ser atualizada para refletir mudanças no sistema ou na legislação. A data da última atualização aparece no início do documento. Quando ela mudar, o sistema pedirá que você leia e aceite a nova versão no próximo acesso, e o aceite fica registrado na sua conta.
