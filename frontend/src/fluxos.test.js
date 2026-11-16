import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import App from "./App";
import { versaoDosTermos } from "./services/termos";

jest.mock("./Componentes/ModalDocumento", () => () => null);
jest.mock("./Componentes/ModalPoliticaReserva", () => {
  const React = require("react");
  return ({ livro, aoConfirmar, aoFechar }) =>
    React.createElement(
      "div",
      { role: "dialog", "aria-label": "Política de Uso de Reservas" },
      React.createElement("span", null, livro.titulo),
      React.createElement("button", { type: "button", onClick: () => aoConfirmar("versao-teste") }, "Confirmar reserva"),
      React.createElement("button", { type: "button", onClick: aoFechar }, "Cancelar política")
    );
});

const API = "http://localhost:8080";
const ID_USUARIO = "11111111-1111-4111-8111-111111111111";
const ID_LIVRO = "22222222-2222-4222-8222-222222222222";
const ID_LIVRO_SEM_VENDA = "33333333-3333-4333-8333-333333333333";
const ID_PEDIDO = "44444444-4444-4444-8444-444444444444";

const livro = (id, titulo, extras = {}) => ({
  id,
  titulo,
  autores: ["Autor"],
  anoLancamento: "2020",
  numeroPagina: 100,
  capaUrl: null,
  materia: "Ti",
  preco: 40,
  estoqueReserva: 2,
  estoqueCompra: 3,
  ...extras,
});

const tokenValido = () =>
  "x." + btoa(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 3600 })) + ".y";

const entrarComo = (tipo) => {
  localStorage.setItem(
    "pallanthir:usuario",
    JSON.stringify({ id: ID_USUARIO, nome: "Ana", email: "ana@teste.com", tipo, token: tokenValido() })
  );
  localStorage.setItem("pallanthir:ultimaAtividade", String(Date.now()));
};

const pedidoPendente = {
  id: ID_PEDIDO,
  status: "AGUARDANDO_PAGAMENTO",
  total: 40,
  dataCriacao: "2026-11-12T10:00:00",
  dataExpiracao: "2026-11-12T10:30:00",
  linkPagamento: "https://www.mercadopago.com.br/checkout/v1/redirect?order_id=ORD7",
  itens: [{ livroId: ID_LIVRO, titulo: "Clean Code", quantidade: 1, precoUnitario: 40 }],
};

let chamadas;

const simularApi = (rotasExtras = {}) => {
  chamadas = [];
  const rotas = {
    "GET /livros": () => [200, [livro(ID_LIVRO, "Clean Code", { isbn10: "8595084742", isbn13: "9788595084742" }), livro(ID_LIVRO_SEM_VENDA, "Sem Venda", { estoqueCompra: 0 })]],
    "GET /livros/materias": () => [200, [{ nome: "TI", rotulo: "Ti", quantidade: 2 }]],
    ["GET /usuarios/" + ID_USUARIO + "/favoritos"]: () => [200, []],
    ["GET /reservas/usuario/" + ID_USUARIO]: () => [200, []],
    "GET /carrinho": () => [200, { itens: [], total: 0 }],
    "GET /pedidos": () => [200, []],
    "GET /avaliacoes/ranking": () => [200, []],
    "GET /usuarios/termos": () => [200, { versaoAceita: versaoDosTermos("[]", "[]"), dataAceite: "2026-11-01T10:00:00" }],
    "POST /usuarios/renovar-token": () => [200, { token: tokenValido(), usuario: { nome: "Ana" } }],
    ...rotasExtras,
  };
  global.fetch = jest.fn((url, opcoes = {}) => {
    const caminho = url.replace(API, "").split("?")[0];
    const metodo = opcoes.method || "GET";
    const corpo = opcoes.body ? JSON.parse(opcoes.body) : null;
    chamadas.push({ metodo, caminho, corpo });
    const rota = rotas[metodo + " " + caminho];
    const [status, dados] = rota ? rota(corpo) : [200, []];
    return Promise.resolve({
      ok: status < 400,
      status,
      text: () => Promise.resolve(dados === undefined ? "" : JSON.stringify(dados)),
    });
  });
};

const chamadasPara = (metodo, caminho) =>
  chamadas.filter((c) => c.metodo === metodo && c.caminho === caminho);

const carrinhoCom = (quantidade, preco = 40) => ({
  itens: [
    {
      livroId: ID_LIVRO,
      titulo: "Clean Code",
      capaUrl: null,
      precoUnitario: preco,
      quantidade,
      subtotal: preco * quantidade,
    },
  ],
  total: preco * quantidade,
});

const abrirCarrinho = async () => {
  const botoes = await screen.findAllByRole("button", { name: /^Carrinho/ });
  fireEvent.click(botoes[0]);
  await screen.findByRole("heading", { name: "Meu carrinho" });
};

const localizacaoOriginal = window.location;

beforeAll(() => {
  window.scrollTo = jest.fn();
});

beforeEach(() => {
  localStorage.clear();
  window.history.replaceState(null, "", "/");
});

afterEach(() => {
  if (window.location !== localizacaoOriginal) {
    delete window.location;
    window.location = localizacaoOriginal;
  }
});

test("visitante é levado ao login ao tentar comprar", async () => {
  simularApi();
  render(<App />);

  const botoes = await screen.findAllByRole("button", { name: "Adicionar ao carrinho" });
  fireEvent.click(botoes[0]);

  expect(await screen.findByRole("heading", { name: "Entrar" })).toBeInTheDocument();
  expect(chamadasPara("POST", "/carrinho/itens")).toHaveLength(0);
});

test("estudante soma a quantidade ao adicionar o mesmo livro de novo", async () => {
  let quantidade = 0;
  simularApi({
    "POST /carrinho/itens": (corpo) => {
      quantidade = corpo.quantidade;
      return [200, carrinhoCom(quantidade)];
    },
    "GET /carrinho": () => [200, quantidade ? carrinhoCom(quantidade) : { itens: [], total: 0 }],
  });
  entrarComo("ESTUDANTE");
  render(<App />);

  const botao = (await screen.findAllByRole("button", { name: "Adicionar ao carrinho" }))[0];
  fireEvent.click(botao);
  await waitFor(() => expect(chamadasPara("POST", "/carrinho/itens")).toHaveLength(1));
  await screen.findAllByRole("button", { name: /Carrinho \(1\)/ });
  fireEvent.click(botao);
  await waitFor(() => expect(chamadasPara("POST", "/carrinho/itens")).toHaveLength(2));

  expect(chamadasPara("POST", "/carrinho/itens").map((c) => c.corpo.quantidade)).toEqual([1, 2]);
  expect((await screen.findAllByRole("button", { name: /Carrinho \(2\)/ })).length).toBeGreaterThan(0);
});

test("livro sem estoque para venda aparece indisponível para compra", async () => {
  simularApi();
  entrarComo("ESTUDANTE");
  render(<App />);

  const indisponivel = await screen.findByRole("button", { name: "Indisponível para compra" });

  expect(indisponivel).toBeDisabled();
});

test("carrinho com pedido pendente avisa e fica bloqueado", async () => {
  simularApi({
    "GET /carrinho": () => [200, carrinhoCom(1)],
    "GET /pedidos": () => [200, [pedidoPendente]],
  });
  entrarComo("ESTUDANTE");
  render(<App />);

  await abrirCarrinho();

  expect(await screen.findByText(/O pedido #44444444 está aguardando pagamento/)).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Remover" })).toBeDisabled();
  expect(screen.getByRole("button", { name: "Aumentar quantidade" })).toBeDisabled();
});

test("pagar cria o pedido, renova o token e leva ao Mercado Pago", async () => {
  const assign = jest.fn();
  delete window.location;
  window.location = { ...localizacaoOriginal, pathname: "/", search: "", assign };
  simularApi({
    "GET /carrinho": () => [200, carrinhoCom(1)],
    "POST /pedidos/checkout": () => [200, pedidoPendente],
  });
  entrarComo("ESTUDANTE");
  render(<App />);

  await abrirCarrinho();
  fireEvent.click(screen.getByRole("button", { name: "Pagar com Mercado Pago" }));

  await waitFor(() => expect(assign).toHaveBeenCalledWith(pedidoPendente.linkPagamento));
  expect(chamadasPara("POST", "/pedidos/checkout")).toHaveLength(1);
  expect(chamadasPara("POST", "/usuarios/renovar-token")).toHaveLength(1);
});

test("erro no checkout é exibido e o carrinho continua na tela", async () => {
  simularApi({
    "GET /carrinho": () => [200, carrinhoCom(1)],
    "POST /pedidos/checkout": () => [409, { mensagem: "O estoque de Clean Code está indisponível" }],
  });
  entrarComo("ESTUDANTE");
  render(<App />);

  await abrirCarrinho();
  fireEvent.click(screen.getByRole("button", { name: "Pagar com Mercado Pago" }));

  expect(await screen.findByText("O estoque de Clean Code está indisponível")).toBeInTheDocument();
  expect(screen.getByRole("heading", { name: "Meu carrinho" })).toBeInTheDocument();
});

test("retorno do Mercado Pago abre Meus pedidos com aviso e limpa a URL", async () => {
  window.history.replaceState(null, "", "/compra/sucesso");
  simularApi({ "GET /pedidos": () => [200, [pedidoPendente]] });
  entrarComo("ESTUDANTE");
  render(<App />);

  expect(await screen.findByRole("heading", { name: "Meus pedidos" })).toBeInTheDocument();
  expect(screen.getByText(/Recebemos o retorno do Mercado Pago/)).toBeInTheDocument();
  expect(await screen.findByText("Pedido #44444444")).toBeInTheDocument();
  expect(window.location.pathname).toBe("/");
});

test("retorno sem sessão pede login e explica que o pagamento não foi afetado", async () => {
  window.history.replaceState(null, "", "/compra/sucesso");
  simularApi();
  render(<App />);

  expect(await screen.findByRole("button", { name: "Entre na sua conta" })).toBeInTheDocument();
  expect(screen.getByText(/não afeta o seu pagamento/)).toBeInTheDocument();
  expect(chamadasPara("GET", "/pedidos")).toHaveLength(0);
});

test("sessão antiga com id numérico é descartada", async () => {
  simularApi();
  localStorage.setItem(
    "pallanthir:usuario",
    JSON.stringify({ id: 1, nome: "Ana", email: "ana@teste.com", tipo: "ESTUDANTE", token: tokenValido() })
  );
  localStorage.setItem("pallanthir:ultimaAtividade", String(Date.now()));
  render(<App />);

  expect(await screen.findByRole("button", { name: "Entrar" })).toBeInTheDocument();
  expect(localStorage.getItem("pallanthir:usuario")).toBeNull();
  expect(chamadas.some((c) => c.caminho.includes("/usuarios/1/"))).toBe(false);
});

test("página do livro mostra os ISBNs e traço quando não há", async () => {
  simularApi({ ["GET /avaliacoes/livro/" + ID_LIVRO]: () => [200, { media: 0, total: 0, avaliacoes: [] }] });
  render(<App />);

  fireEvent.click(await screen.findByRole("heading", { name: "Clean Code" }));

  expect(await screen.findByText("ISBN-10")).toBeInTheDocument();
  expect(screen.getByText("8595084742")).toBeInTheDocument();
  expect(screen.getByText("978-8595084742")).toBeInTheDocument();
});

test("menu de navegação abre e fecha pelo botão da direita", async () => {
  simularApi();
  entrarComo("ESTUDANTE");
  render(<App />);

  const botao = await screen.findByRole("button", { name: "Abrir menu" });
  expect(botao).toHaveAttribute("aria-expanded", "false");

  fireEvent.click(botao);
  expect(screen.getByRole("button", { name: "Fechar menu" })).toHaveAttribute("aria-expanded", "true");
  expect(document.getElementById("menu-navegacao")).toHaveClass("aberto");

  fireEvent.click(screen.getByRole("button", { name: /^Reservas/ }));
  expect(await screen.findByRole("button", { name: "Abrir menu" })).toHaveAttribute("aria-expanded", "false");
});

test("exclusão de conta com reserva ativa mostra o aviso e não chama a API", async () => {
  simularApi({
    ["GET /reservas/usuario/" + ID_USUARIO]: () => [
      200,
      [{ id: "r1", livroId: ID_LIVRO, tituloLivro: "Clean Code", dataReserva: "2026-11-12T10:00:00", status: "RESERVADO" }],
    ],
  });
  entrarComo("ESTUDANTE");
  render(<App />);

  fireEvent.click(await screen.findByTitle("Minha conta"));
  await screen.findByRole("button", { name: /^Reservas \(1\)/ });
  fireEvent.click(await screen.findByRole("button", { name: "Excluir minha conta" }));

  expect(await screen.findByRole("alertdialog")).toHaveTextContent("Não é possível excluir sua conta");
  expect(screen.getByRole("button", { name: "Ver minhas reservas" })).toBeInTheDocument();
  expect(chamadasPara("DELETE", "/usuarios/deletar/" + ID_USUARIO)).toHaveLength(0);

  fireEvent.click(screen.getByRole("button", { name: "Entendi" }));
  expect(screen.queryByRole("alertdialog")).not.toBeInTheDocument();
});

test("exclusão de conta pede confirmação na janela e exclui ao confirmar", async () => {
  simularApi({ ["DELETE /usuarios/deletar/" + ID_USUARIO]: () => [204, undefined] });
  entrarComo("ESTUDANTE");
  render(<App />);

  fireEvent.click(await screen.findByTitle("Minha conta"));
  fireEvent.click(await screen.findByRole("button", { name: "Excluir minha conta" }));

  expect(await screen.findByRole("alertdialog")).toHaveTextContent("Excluir sua conta?");
  fireEvent.click(screen.getByRole("button", { name: "Cancelar" }));
  expect(screen.queryByRole("alertdialog")).not.toBeInTheDocument();
  expect(chamadasPara("DELETE", "/usuarios/deletar/" + ID_USUARIO)).toHaveLength(0);

  fireEvent.click(screen.getByRole("button", { name: "Excluir minha conta" }));
  fireEvent.click(await screen.findByRole("button", { name: "Excluir conta" }));

  expect(await screen.findByText("Sua conta foi excluída.")).toBeInTheDocument();
  expect(chamadasPara("DELETE", "/usuarios/deletar/" + ID_USUARIO)).toHaveLength(1);
});

test("acervo mostra 18 livros por página e troca de página", async () => {
  const muitos = Array.from({ length: 20 }, (_, i) =>
    livro("55555555-5555-4555-8555-" + String(i).padStart(12, "0"), "Livro " + (i + 1))
  );
  simularApi({ "GET /livros": () => [200, muitos] });
  render(<App />);

  expect(await screen.findByRole("heading", { name: "Livro 1" })).toBeInTheDocument();
  expect(screen.getByRole("heading", { name: "Livro 18" })).toBeInTheDocument();
  expect(screen.queryByRole("heading", { name: "Livro 19" })).not.toBeInTheDocument();
  expect(screen.getByText("Mostrando 1–18 de 20 livros")).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "← Anterior" })).toBeDisabled();

  fireEvent.click(screen.getByRole("button", { name: "Página 2" }));

  expect(await screen.findByRole("heading", { name: "Livro 19" })).toBeInTheDocument();
  expect(screen.queryByRole("heading", { name: "Livro 1" })).not.toBeInTheDocument();
  expect(screen.getByText("Mostrando 19–20 de 20 livros")).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Próxima →" })).toBeDisabled();
});

test("acervo com até 18 livros não mostra a paginação", async () => {
  simularApi();
  render(<App />);

  await screen.findByRole("heading", { name: "Clean Code" });
  expect(screen.queryByRole("navigation", { name: "Páginas de livros" })).not.toBeInTheDocument();
});

test("cadastrar livro aparece só na tela principal do funcionário", async () => {
  simularApi({ "GET /logs": () => [200, []], "GET /usuarios": () => [200, []] });
  entrarComo("FUNCIONARIO");
  render(<App />);

  await screen.findByRole("heading", { name: "Clean Code" });
  expect(screen.getAllByRole("button", { name: "+ Cadastrar livro" })).toHaveLength(1);

  fireEvent.click(screen.getByRole("button", { name: "Logs" }));
  await screen.findByRole("heading", { name: "Logs do sistema" });
  expect(screen.queryByRole("button", { name: "+ Cadastrar livro" })).not.toBeInTheDocument();
});

test("logs podem ser filtrados por ação e por período", async () => {
  const log = (id, acao, dataHora, descricao) => ({ id, acao, dataHora, descricao, usuarioEmail: "ana@teste.com" });
  simularApi({
    "GET /logs": () => [
      200,
      [
        log("1", "ACEITE_TERMOS", "2026-11-14T09:00:00", "Aceitou os termos"),
        log("2", "SUCESSO_LOGIN", "2026-11-10T08:00:00", "Login feito"),
        log("3", "SUCESSO_LOGIN", "2026-11-01T08:00:00", "Login antigo"),
      ],
    ],
    "GET /usuarios": () => [200, []],
  });
  entrarComo("FUNCIONARIO");
  render(<App />);

  fireEvent.click(await screen.findByRole("button", { name: "Logs" }));
  expect(await screen.findByText("Login antigo")).toBeInTheDocument();
  expect(screen.getByText("3 registros")).toBeInTheDocument();

  fireEvent.change(screen.getByRole("combobox", { name: "Filtrar por ação" }), { target: { value: "SUCESSO_LOGIN" } });
  expect(screen.queryByText("Aceitou os termos")).not.toBeInTheDocument();
  expect(await screen.findByText("2 registros com os filtros escolhidos")).toBeInTheDocument();
  expect(global.fetch.mock.calls.some(([url]) => String(url).endsWith("/logs?acao=SUCESSO_LOGIN"))).toBe(true);

  fireEvent.change(screen.getByLabelText("De"), { target: { value: "2026-11-05" } });
  expect(await screen.findByText("Login feito")).toBeInTheDocument();
  expect(global.fetch.mock.calls.some(([url]) => String(url).endsWith("/logs?acao=SUCESSO_LOGIN&de=2026-11-05"))).toBe(true);
  expect(screen.queryByText("Login antigo")).not.toBeInTheDocument();

  fireEvent.change(screen.getByLabelText("Até"), { target: { value: "2026-11-09" } });
  expect(await screen.findByText("Nenhum registro encontrado.")).toBeInTheDocument();

  fireEvent.click(screen.getByRole("button", { name: /Limpar filtros/ }));
  expect(await screen.findByText("3 registros")).toBeInTheDocument();
});

test("usuário precisa ler e aceitar os termos quando eles mudam", async () => {
  simularApi({
    "GET /usuarios/termos": () => [200, { versaoAceita: "versao-antiga", dataAceite: "2026-01-01T10:00:00" }],
    "POST /usuarios/termos/aceite": (corpo) => [200, { versaoAceita: corpo.versao, dataAceite: "2026-11-14T10:00:00" }],
  });
  entrarComo("ESTUDANTE");
  render(<App />);

  expect(await screen.findByRole("alertdialog")).toHaveTextContent("Os termos foram atualizados");
  const aceitar = screen.getByRole("button", { name: "Aceitar e continuar" });
  expect(aceitar).toBeDisabled();
  expect(screen.getByRole("checkbox")).toBeDisabled();

  fireEvent.click(screen.getByRole("button", { name: "Termos de Uso" }));
  fireEvent.click(screen.getByRole("button", { name: "Política de Privacidade" }));
  fireEvent.click(await screen.findByRole("checkbox"));
  expect(aceitar).toBeEnabled();
  fireEvent.click(aceitar);

  expect(await screen.findByText("Obrigado! Você aceitou a nova versão dos termos.")).toBeInTheDocument();
  expect(screen.queryByRole("alertdialog")).not.toBeInTheDocument();
  expect(chamadasPara("POST", "/usuarios/termos/aceite")[0].corpo).toEqual({ versao: versaoDosTermos("[]", "[]") });
});

test("usuário com os termos em dia não vê a janela de aceite", async () => {
  simularApi();
  entrarComo("ESTUDANTE");
  render(<App />);

  await screen.findByRole("heading", { name: "Clean Code" });
  await waitFor(() => expect(chamadasPara("GET", "/usuarios/termos")).toHaveLength(1));
  expect(screen.queryByRole("alertdialog")).not.toBeInTheDocument();
});

test("versão dos termos muda quando o texto muda e ignora quebra de linha do Windows", () => {
  expect(versaoDosTermos("Termos\r\nv1", "Política")).toBe(versaoDosTermos("Termos\nv1", "Política"));
  expect(versaoDosTermos("Termos v1", "Política")).not.toBe(versaoDosTermos("Termos v2", "Política"));
  expect(versaoDosTermos("Termos", "Política")).toMatch(/^[0-9a-f]{28}$/);
});

test("funcionário edita o livro direto na página e salva", async () => {
  simularApi({
    ["GET /avaliacoes/livro/" + ID_LIVRO]: () => [200, { media: 0, total: 0, avaliacoes: [] }],
    ["PUT /livros/atualizar/" + ID_LIVRO]: (corpo) => [200, livro(ID_LIVRO, corpo.titulo, corpo)],
  });
  entrarComo("FUNCIONARIO");
  render(<App />);

  fireEvent.click(await screen.findByRole("heading", { name: "Clean Code" }));
  expect(screen.queryByRole("textbox", { name: "Título" })).not.toBeInTheDocument();

  fireEvent.click(await screen.findByRole("button", { name: "✎ Atualizar livro" }));
  fireEvent.change(screen.getByRole("textbox", { name: "Título" }), { target: { value: "Código Limpo" } });
  fireEvent.change(screen.getByRole("spinbutton", { name: "Páginas" }), { target: { value: "425" } });
  fireEvent.change(screen.getByRole("textbox", { name: "ISBN-13" }), { target: { value: "978-8576082675" } });
  fireEvent.click(screen.getByRole("button", { name: "Salvar alterações" }));

  expect(await screen.findByText('"Código Limpo" foi atualizado.')).toBeInTheDocument();
  const corpo = chamadasPara("PUT", "/livros/atualizar/" + ID_LIVRO)[0].corpo;
  expect(corpo).toMatchObject({ titulo: "Código Limpo", numeroPagina: 425, isbn13: "978-8576082675" });
  expect(screen.queryByRole("textbox", { name: "Título" })).not.toBeInTheDocument();
  expect(screen.getByRole("button", { name: "✎ Atualizar livro" })).toBeInTheDocument();
});

test("cancelar a edição do livro descarta as mudanças", async () => {
  simularApi({ ["GET /avaliacoes/livro/" + ID_LIVRO]: () => [200, { media: 0, total: 0, avaliacoes: [] }] });
  entrarComo("FUNCIONARIO");
  render(<App />);

  fireEvent.click(await screen.findByRole("heading", { name: "Clean Code" }));
  fireEvent.click(await screen.findByRole("button", { name: "✎ Atualizar livro" }));
  fireEvent.change(screen.getByRole("textbox", { name: "Título" }), { target: { value: "Outro título" } });
  fireEvent.click(screen.getByRole("button", { name: "Cancelar" }));

  expect(screen.getByRole("heading", { level: 1, name: "Clean Code" })).toBeInTheDocument();
  expect(chamadasPara("PUT", "/livros/atualizar/" + ID_LIVRO)).toHaveLength(0);
});

test.each([
  ["Reservas", /^Reservas/, "para ver suas reservas."],
  ["Favoritos", /^Favoritos/, "para ver os livros que você favoritou."],
])("visitante em %s vê o link para entrar na conta", async (_, botao, texto) => {
  simularApi();
  render(<App />);

  fireEvent.click(await screen.findByRole("button", { name: botao }));
  expect(await screen.findByText(texto, { exact: false })).toBeInTheDocument();

  fireEvent.click(screen.getByRole("button", { name: "Entre na sua conta" }));
  expect(await screen.findByRole("heading", { name: "Entrar" })).toBeInTheDocument();
});

test("sair fica só em Minha conta e encerra a sessão", async () => {
  simularApi();
  entrarComo("ESTUDANTE");
  render(<App />);

  await screen.findByRole("heading", { name: "Clean Code" });
  expect(screen.queryByRole("button", { name: /^Sair/ })).not.toBeInTheDocument();

  fireEvent.click(screen.getByTitle("Minha conta"));
  fireEvent.click(await screen.findByRole("button", { name: /Sair da conta/ }));

  expect(await screen.findByText("Você saiu da sua conta.")).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Entrar" })).toBeInTheDocument();
});

test("detalhes do pedido mostram os livros com capa e o total", async () => {
  const aprovado = {
    ...pedidoPendente,
    status: "APROVADO",
    total: 120,
    itens: [
      { livroId: ID_LIVRO, titulo: "Clean Code", quantidade: 2, precoUnitario: 40, capaUrl: "https://covers.openlibrary.org/b/isbn/1-L.jpg" },
      { livroId: ID_LIVRO_SEM_VENDA, titulo: "Sem Venda", quantidade: 1, precoUnitario: 40, capaUrl: null },
    ],
  };
  simularApi({ "GET /pedidos": () => [200, [aprovado]] });
  entrarComo("ESTUDANTE");
  render(<App />);

  fireEvent.click(await screen.findByRole("button", { name: "Pedidos" }));
  fireEvent.click(await screen.findByRole("button", { name: "Ver detalhes →" }));

  const janela = await screen.findByRole("dialog", { name: "Pedido #44444444" });
  expect(within(janela).getByAltText("Capa de Clean Code")).toHaveAttribute("src", "https://covers.openlibrary.org/b/isbn/1-L.jpg");
  expect(within(janela).getByRole("img", { name: "Capa genérica de Sem Venda" })).toBeInTheDocument();
  expect(within(janela).getByText("2 unidades ×", { exact: false })).toBeInTheDocument();
  expect(within(janela).getByText("Pagamento aprovado. Retire os livros no balcão da biblioteca.")).toBeInTheDocument();
  expect(within(janela).getByText("R$ 120,00")).toBeInTheDocument();

  fireEvent.click(within(janela).getByRole("button", { name: "Fechar" }));
  expect(screen.queryByRole("dialog", { name: "Pedido #44444444" })).not.toBeInTheDocument();
});

test("ranking pode ser filtrado por matéria", async () => {
  const item = (id, titulo) => ({ id, titulo, capaUrl: null, media: 4.5, total: 3 });
  simularApi({
    "GET /livros/materias": () => [200, [{ nome: "TI", rotulo: "Ti", quantidade: 2 }, { nome: "DIREITO", rotulo: "Direito", quantidade: 1 }]],
  });
  const fetchOriginal = global.fetch;
  global.fetch = jest.fn((url, opcoes) => {
    if (String(url).includes("/avaliacoes/ranking?materia=DIREITO")) {
      return Promise.resolve({ ok: true, status: 200, text: () => Promise.resolve(JSON.stringify([item("d1", "Código Civil Comentado")])) });
    }
    if (String(url).endsWith("/avaliacoes/ranking")) {
      return Promise.resolve({ ok: true, status: 200, text: () => Promise.resolve(JSON.stringify([item("t1", "Clean Code"), item("d1", "Código Civil Comentado")])) });
    }
    return fetchOriginal(url, opcoes);
  });
  render(<App />);

  fireEvent.click(await screen.findByRole("button", { name: /^Ranking/ }));
  expect(await screen.findByText("Código Civil Comentado", { selector: ".ranking-info strong" })).toBeInTheDocument();
  const filtrar = screen.getByRole("button", { name: /^Filtrar/ });
  expect(filtrar).toHaveAttribute("aria-expanded", "false");
  fireEvent.click(filtrar);
  expect(filtrar).toHaveAttribute("aria-expanded", "true");
  expect(screen.getByRole("button", { name: "Todas as matérias" })).toHaveAttribute("aria-pressed", "true");

  fireEvent.click(screen.getByRole("button", { name: "Direito" }));
  expect(screen.getByRole("button", { name: /^Filtrar/ })).toHaveAttribute("aria-expanded", "false");

  expect(await screen.findByRole("heading", { name: "Top 10 mais bem avaliados em Direito" })).toBeInTheDocument();
  await waitFor(() => expect(screen.queryByText("Clean Code", { selector: ".ranking-info strong" })).not.toBeInTheDocument());
  expect(screen.getByText("Código Civil Comentado", { selector: ".ranking-info strong" })).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Filtrar Direito" })).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Direito" })).toHaveAttribute("aria-pressed", "true");
  expect(global.fetch.mock.calls.some(([url]) => String(url).endsWith("/avaliacoes/ranking?materia=DIREITO"))).toBe(true);
});

test("estudante solicita reembolso de um pedido pago e ainda não retirado", async () => {
  let reembolsado = false;
  const pago = { ...pedidoPendente, status: "APROVADO", total: 40 };
  simularApi({
    "GET /pedidos": () => [200, [reembolsado ? { ...pago, status: "REEMBOLSADO", dataReembolso: "2026-11-15T10:00:00" } : pago]],
    ["POST /pedidos/" + ID_PEDIDO + "/reembolso"]: () => {
      reembolsado = true;
      return [200, { ...pago, status: "REEMBOLSADO" }];
    },
  });
  entrarComo("ESTUDANTE");
  render(<App />);

  fireEvent.click(await screen.findByRole("button", { name: "Pedidos" }));
  expect(await screen.findByText("Pago · aguardando retirada")).toBeInTheDocument();
  expect(screen.queryByRole("button", { name: "Solicitar reembolso" })).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Ver detalhes →" }));
  fireEvent.click(await screen.findByRole("button", { name: "Solicitar reembolso" }));

  const dialogo = await screen.findByRole("alertdialog");
  expect(dialogo).toHaveTextContent("Solicitar reembolso?");
  fireEvent.click(within(dialogo).getByRole("button", { name: "Confirmar reembolso" }));

  expect(await screen.findByText(/Reembolso do pedido #44444444 solicitado/)).toBeInTheDocument();
  expect(await screen.findByText("Reembolsado")).toBeInTheDocument();
  expect(chamadasPara("POST", "/pedidos/" + ID_PEDIDO + "/reembolso")).toHaveLength(1);
  expect(screen.queryByRole("button", { name: "Solicitar reembolso" })).not.toBeInTheDocument();
});

test("funcionário registra a retirada de uma compra na gestão", async () => {
  simularApi({
    "GET /pedidos/pendentes-retirada": () => [
      200,
      [{ id: ID_PEDIDO, nomeEstudante: "Ana Souza", total: 80, dataCriacao: "2026-11-15T09:00:00", itens: [{ livroId: ID_LIVRO, titulo: "Clean Code", quantidade: 2, precoUnitario: 40, capaUrl: null }] }],
    ],
    ["PUT /pedidos/" + ID_PEDIDO + "/entregar"]: () => [200, { ...pedidoPendente, status: "ENTREGUE" }],
  });
  entrarComo("FUNCIONARIO");
  render(<App />);

  simularApi({
    "GET /usuarios": () => [200, []],
    "GET /pedidos/pendentes-retirada": () => [
      200,
      [{ id: ID_PEDIDO, nomeEstudante: "Ana Souza", total: 80, dataCriacao: "2026-11-15T09:00:00", itens: [{ livroId: ID_LIVRO, titulo: "Clean Code", quantidade: 2, precoUnitario: 40, capaUrl: null }] }],
    ],
    ["PUT /pedidos/" + ID_PEDIDO + "/entregar"]: () => [200, { ...pedidoPendente, status: "ENTREGUE" }],
  });
  fireEvent.click(await screen.findByRole("button", { name: "Gestão" }));
  const abaCompras = await screen.findByRole("tab", { name: /^Compras para retirar/ });
  expect(screen.getByRole("tab", { name: "Reservas" })).toHaveAttribute("aria-selected", "true");
  fireEvent.click(abaCompras);
  expect(abaCompras).toHaveAttribute("aria-selected", "true");
  expect(await screen.findByText("Ana Souza")).toBeInTheDocument();
  expect(screen.getByText("2 unidades")).toBeInTheDocument();

  fireEvent.click(screen.getByRole("button", { name: "Registrar retirada" }));

  expect(await screen.findByText("Retirada do pedido #44444444 registrada para Ana Souza.")).toBeInTheDocument();
  expect(screen.getByText("Nenhum pedido aguardando retirada.")).toBeInTheDocument();
  expect(chamadasPara("PUT", "/pedidos/" + ID_PEDIDO + "/entregar")).toHaveLength(1);
});

test("conta com pedido aguardando retirada não pode ser excluída", async () => {
  simularApi({ "GET /pedidos": () => [200, [{ ...pedidoPendente, status: "APROVADO" }]] });
  entrarComo("ESTUDANTE");
  render(<App />);

  await screen.findByRole("heading", { name: "Clean Code" });
  await waitFor(() => expect(chamadasPara("GET", "/pedidos").length).toBeGreaterThan(0));
  fireEvent.click(screen.getByTitle("Minha conta"));
  fireEvent.click(await screen.findByRole("button", { name: "Excluir minha conta" }));

  expect(await screen.findByRole("alertdialog")).toHaveTextContent("Você tem pedidos em andamento");
  expect(chamadasPara("DELETE", "/usuarios/deletar/" + ID_USUARIO)).toHaveLength(0);
});

test("requisitos da senha só aparecem quando a senha está errada", async () => {
  simularApi();
  render(<App />);

  fireEvent.click(await screen.findByRole("button", { name: "Entrar" }));
  fireEvent.click(await screen.findByRole("button", { name: "Criar conta" }));
  expect(screen.queryByRole("list", { name: "Requisitos da senha" })).not.toBeInTheDocument();

  const [senha, confirmacao] = document.querySelectorAll(".campo-senha input");
  fireEvent.change(screen.getByLabelText("Nome"), { target: { value: "Ana" } });
  fireEvent.change(screen.getByLabelText("E-mail"), { target: { value: "ana@teste.com" } });
  fireEvent.change(senha, { target: { value: "fraca" } });
  fireEvent.change(confirmacao, { target: { value: "fraca" } });
  fireEvent.click(screen.getByRole("checkbox"));
  fireEvent.click(screen.getByRole("button", { name: "Criar minha conta" }));

  expect(await screen.findByRole("list", { name: "Requisitos da senha" })).toBeInTheDocument();
  expect(screen.getByText("A senha não atende a todos os requisitos.")).toBeInTheDocument();
  expect(chamadasPara("POST", "/usuarios/cadastro")).toHaveLength(0);
});

test("aba de reservas da gestão registra entrega e devolução", async () => {
  const aberta = (id, status, titulo, aluno, usuarioId) => ({
    id, livroId: ID_LIVRO, tituloLivro: titulo, capaUrl: null, dataReserva: "2026-11-15T09:00:00",
    status, usuarioId, nomeEstudante: aluno, emailEstudante: aluno.toLowerCase() + "@teste.com",
  });
  simularApi({
    "GET /reservas/em-aberto": () => [200, [aberta("r1", "RESERVADO", "Clean Code", "Ana", "u1"), aberta("r2", "ENTREGUE", "Sem Venda", "Bruno", "u2")]],
    "PUT /reservas/r1/entregar": () => [200, undefined],
    "PUT /reservas/r2/devolver": () => [200, undefined],
  });
  entrarComo("FUNCIONARIO");
  render(<App />);

  fireEvent.click(await screen.findByRole("button", { name: "Gestão" }));
  const abaReservas = await screen.findByRole("tab", { name: /^Reservas/ });
  expect(abaReservas).toHaveAttribute("aria-selected", "true");
  expect(await screen.findByText("Aguardando retirada")).toBeInTheDocument();
  expect(screen.getByText("Com o estudante")).toBeInTheDocument();
  expect(within(abaReservas).getByText("1")).toBeInTheDocument();

  fireEvent.click(screen.getByRole("button", { name: "Registrar entrega" }));
  expect(await screen.findByText('"Clean Code" foi entregue para Ana.')).toBeInTheDocument();
  expect(screen.getAllByText("Com o estudante")).toHaveLength(2);

  fireEvent.click(screen.getAllByRole("button", { name: "Registrar devolução" })[1]);
  const janela = await screen.findByRole("dialog", { name: "Registrar devolução" });
  expect(within(janela).getByRole("radio", { name: /Em bom estado/ })).toHaveAttribute("aria-checked", "true");
  fireEvent.click(within(janela).getByRole("button", { name: "Registrar devolução" }));
  expect(await screen.findByText('"Sem Venda" foi devolvido por Bruno.')).toBeInTheDocument();
  expect(chamadasPara("PUT", "/reservas/r2/devolver")[0].corpo).toEqual({ danificado: false, valorMulta: null, observacao: null });
  expect(screen.queryByText("Bruno", { selector: ".retirada-cabecalho strong" })).not.toBeInTheDocument();
  expect(chamadasPara("PUT", "/reservas/r1/entregar")).toHaveLength(1);
  expect(chamadasPara("PUT", "/reservas/r2/devolver")).toHaveLength(1);
});

test("filtro por aluno mostra só as compras do aluno escolhido", async () => {
  const compra = (id, aluno, usuarioId) => ({
    id, nomeEstudante: aluno, usuarioId, emailEstudante: aluno + "@teste.com", total: 40,
    dataCriacao: "2026-11-15T09:00:00", itens: [{ livroId: ID_LIVRO, titulo: "Clean Code", quantidade: 1, precoUnitario: 40, capaUrl: null }],
  });
  simularApi({
    "GET /pedidos/pendentes-retirada": () => [200, [
      compra("11111111-aaaa-4aaa-8aaa-000000000001", "Ana", "u1"),
      compra("22222222-aaaa-4aaa-8aaa-000000000002", "Bruno", "u2"),
      compra("33333333-aaaa-4aaa-8aaa-000000000003", "Ana", "u1"),
    ]],
  });
  entrarComo("FUNCIONARIO");
  render(<App />);

  fireEvent.click(await screen.findByRole("button", { name: "Gestão" }));
  fireEvent.click(await screen.findByRole("tab", { name: /^Compras para retirar/ }));
  expect(await screen.findByText("Bruno", { selector: ".retirada-cabecalho strong" })).toBeInTheDocument();

  fireEvent.click(screen.getByRole("button", { name: /^Aluno/ }));
  fireEvent.click(within(screen.getByRole("group", { name: "Filtrar por aluno" })).getByRole("button", { name: "Ana 2" }));

  expect(screen.queryByText("Bruno", { selector: ".retirada-cabecalho strong" })).not.toBeInTheDocument();
  expect(screen.getAllByText("Ana", { selector: ".retirada-cabecalho strong" })).toHaveLength(2);
  expect(screen.getByRole("button", { name: "Aluno Ana" })).toBeInTheDocument();
});

test("reservas e pedidos mostram o prazo para retirada", async () => {
  const daqui = (horas) => {
    const data = new Date(Date.now() + horas * 60 * 60 * 1000);
    const doisDigitos = (n) => String(n).padStart(2, "0");
    return data.getFullYear() + "-" + doisDigitos(data.getMonth() + 1) + "-" + doisDigitos(data.getDate()) +
      "T" + doisDigitos(data.getHours()) + ":" + doisDigitos(data.getMinutes()) + ":00";
  };
  simularApi({
    ["GET /reservas/usuario/" + ID_USUARIO]: () => [
      200,
      [{ id: "r1", livroId: ID_LIVRO, tituloLivro: "Clean Code", dataReserva: daqui(-20), status: "RESERVADO", prazoRetirada: daqui(52) }],
    ],
    "GET /pedidos": () => [200, [{ ...pedidoPendente, status: "APROVADO", prazoRetirada: daqui(5) }]],
  });
  entrarComo("ESTUDANTE");
  render(<App />);

  fireEvent.click(await screen.findByRole("button", { name: /^Reservas/ }));
  expect(await screen.findByText(/faltam 2 dias e [34] h/)).toBeInTheDocument();
  expect(screen.getByText("Se o livro não for retirado até lá, a reserva é cancelada automaticamente.")).toBeInTheDocument();

  fireEvent.click(screen.getByRole("button", { name: "Pedidos" }));
  expect(await screen.findByText(/faltam [45] h/)).toBeInTheDocument();
  expect(screen.getByText("Se a compra não for retirada até lá, ela é reembolsada automaticamente.")).toBeInTheDocument();
  expect(document.querySelector(".prazo-retirada.urgente")).not.toBeNull();
});

test("devolução com dano aplica multa com valor e observação", async () => {
  simularApi({
    "GET /livros": () => [200, [livro(ID_LIVRO, "Clean Code", { preco: 59.9 })]],
    "GET /reservas/em-aberto": () => [200, [{
      id: "r9", livroId: ID_LIVRO, tituloLivro: "Clean Code", capaUrl: null, dataReserva: "2026-11-15T09:00:00",
      status: "ENTREGUE", usuarioId: "u1", nomeEstudante: "Ana", emailEstudante: "ana@teste.com",
    }]],
    "PUT /reservas/r9/devolver": () => [200, undefined],
  });
  entrarComo("FUNCIONARIO");
  render(<App />);

  await screen.findByRole("heading", { name: "Clean Code" });
  fireEvent.click(screen.getByRole("button", { name: "Gestão" }));
  fireEvent.click(await screen.findByRole("button", { name: "Registrar devolução" }));
  const janela = await screen.findByRole("dialog", { name: "Registrar devolução" });
  fireEvent.click(within(janela).getByRole("radio", { name: /Danificado/ }));

  const valor = within(janela).getByLabelText("Valor da multa (R$)");
  expect(valor).toHaveValue("59,90");
  fireEvent.change(valor, { target: { value: "45,50" } });
  fireEvent.change(within(janela).getByLabelText("O que aconteceu? (opcional)"), { target: { value: "Capa rasgada" } });
  fireEvent.click(within(janela).getByRole("button", { name: "Registrar e aplicar multa" }));

  expect(await screen.findByText(/foi devolvido danificado. Multa de R\$\s45,50 aplicada a Ana/)).toBeInTheDocument();
  expect(chamadasPara("PUT", "/reservas/r9/devolver")[0].corpo).toEqual({ danificado: true, valorMulta: 45.5, observacao: "Capa rasgada" });
});

test("estudante com multa pendente vê o aviso e paga pelo Mercado Pago", async () => {
  const multa = {
    id: "m1", livroId: ID_LIVRO, tituloLivro: "Clean Code", capaUrl: null, valor: 59.9, motivo: "Capa rasgada",
    status: "PENDENTE", dataCriacao: "2026-11-15T10:00:00", dataPagamento: null,
  };
  simularApi({
    "GET /multas/minhas": () => [200, [multa]],
    "POST /multas/m1/pagar": () => [200, { ...multa, linkPagamento: "https://www.mercadopago.com.br/checkout/v1/redirect?order_id=ORDM1" }],
  });
  entrarComo("ESTUDANTE");
  const assign = jest.fn();
  delete window.location;
  window.location = { ...localizacaoOriginal, pathname: "/", search: "", assign };
  render(<App />);

  const aviso = await screen.findByRole("alert");
  expect(aviso).toHaveTextContent('porque o livro "Clean Code" foi devolvido danificado');
  expect(aviso).toHaveTextContent("não é possível reservar nem comprar livros");
  fireEvent.click(within(aviso).getByRole("button", { name: "Pagar multa" }));

  await waitFor(() => expect(assign).toHaveBeenCalledWith("https://www.mercadopago.com.br/checkout/v1/redirect?order_id=ORDM1"));
  expect(chamadasPara("POST", "/multas/m1/pagar")).toHaveLength(1);
});

test("funcionário dispensa multa na aba Multas da gestão", async () => {
  simularApi({
    "GET /multas": () => [200, [{
      id: "m2", livroId: ID_LIVRO, tituloLivro: "Clean Code", capaUrl: null, valor: 30, motivo: "Páginas molhadas",
      status: "PENDENTE", dataCriacao: "2026-11-15T10:00:00", usuarioId: "u1", nomeEstudante: "Ana", emailEstudante: "ana@teste.com",
    }]],
    "PUT /multas/m2/dispensar": () => [200, {}],
  });
  entrarComo("FUNCIONARIO");
  render(<App />);

  fireEvent.click(await screen.findByRole("button", { name: "Gestão" }));
  fireEvent.click(await screen.findByRole("tab", { name: /^Multas/ }));
  expect(await screen.findByText(/Páginas molhadas/)).toBeInTheDocument();

  fireEvent.click(screen.getByRole("button", { name: "Dispensar multa" }));
  expect(chamadasPara("PUT", "/multas/m2/dispensar")).toHaveLength(0);
  fireEvent.click(screen.getByRole("button", { name: "Clique de novo para confirmar" }));

  expect(await screen.findByText(/Multa de R\$\s30,00 de Ana dispensada./)).toBeInTheDocument();
  expect(screen.getByText("Nenhuma multa pendente.")).toBeInTheDocument();
});

test("reservar abre a política de reservas e só reserva depois do aceite", async () => {
  simularApi({ ["POST /reservas/" + ID_USUARIO + "/" + ID_LIVRO]: () => [200, undefined] });
  entrarComo("ESTUDANTE");
  render(<App />);

  await screen.findByRole("heading", { name: "Clean Code" });
  fireEvent.click(screen.getAllByRole("button", { name: "Reservar" })[0]);

  const politica = await screen.findByRole("dialog", { name: "Política de Uso de Reservas" });
  expect(chamadasPara("POST", "/reservas/" + ID_USUARIO + "/" + ID_LIVRO)).toHaveLength(0);
  fireEvent.click(within(politica).getByRole("button", { name: "Confirmar reserva" }));

  expect(await screen.findByText('"Clean Code" foi reservado.')).toBeInTheDocument();
  expect(global.fetch.mock.calls.some(([url]) => String(url).endsWith("/reservas/" + ID_USUARIO + "/" + ID_LIVRO + "?politicaVersao=versao-teste"))).toBe(true);
  expect(screen.queryByRole("dialog", { name: "Política de Uso de Reservas" })).not.toBeInTheDocument();
});

test("estudante cancela pedido que ainda aguarda pagamento", async () => {
  let cancelado = false;
  simularApi({
    "GET /pedidos": () => [200, [cancelado ? { ...pedidoPendente, status: "CANCELADO" } : pedidoPendente]],
    ["POST /pedidos/" + ID_PEDIDO + "/cancelar"]: () => {
      cancelado = true;
      return [200, { ...pedidoPendente, status: "CANCELADO" }];
    },
  });
  entrarComo("ESTUDANTE");
  render(<App />);

  fireEvent.click(await screen.findByRole("button", { name: "Pedidos" }));
  fireEvent.click(await screen.findByRole("button", { name: "Cancelar pedido" }));

  const dialogo = await screen.findByRole("alertdialog");
  expect(dialogo).toHaveTextContent("Cancelar o pedido?");
  fireEvent.click(within(dialogo).getByRole("button", { name: "Cancelar pedido" }));

  expect(await screen.findByText("Pedido #44444444 cancelado. Os livros continuam no seu carrinho.")).toBeInTheDocument();
  expect(await screen.findByText("Você cancelou este pedido antes do pagamento.")).toBeInTheDocument();
  expect(chamadasPara("POST", "/pedidos/" + ID_PEDIDO + "/cancelar")).toHaveLength(1);
  expect(screen.queryByRole("button", { name: "Cancelar pedido" })).not.toBeInTheDocument();
});

test("reserva entregue mostra o prazo de devolução e a capa do livro", async () => {
  const daqui = (horas) => {
    const data = new Date(Date.now() + horas * 60 * 60 * 1000);
    const doisDigitos = (n) => String(n).padStart(2, "0");
    return data.getFullYear() + "-" + doisDigitos(data.getMonth() + 1) + "-" + doisDigitos(data.getDate()) +
      "T" + doisDigitos(data.getHours()) + ":" + doisDigitos(data.getMinutes()) + ":00";
  };
  simularApi({
    ["GET /reservas/usuario/" + ID_USUARIO]: () => [200, [
      { id: "r1", livroId: ID_LIVRO, tituloLivro: "Clean Code", capaUrl: "https://covers.openlibrary.org/b/isbn/1-L.jpg", dataReserva: daqui(-50), dataEntrega: daqui(-30), status: "ENTREGUE", prazoDevolucao: daqui(138) },
      { id: "r2", livroId: ID_LIVRO_SEM_VENDA, tituloLivro: "Sem Venda", capaUrl: null, dataReserva: daqui(-300), dataEntrega: daqui(-200), status: "ENTREGUE", prazoDevolucao: daqui(-32) },
    ]],
  });
  entrarComo("ESTUDANTE");
  render(<App />);

  fireEvent.click(await screen.findByRole("button", { name: /^Reservas/ }));
  expect(await screen.findByText(/faltam 5 dias e (17|18) h/)).toBeInTheDocument();
  expect(screen.getByText(/devolução atrasada/)).toBeInTheDocument();
  expect(screen.getByAltText("Capa de Clean Code")).toHaveAttribute("src", "https://covers.openlibrary.org/b/isbn/1-L.jpg");
  expect(screen.getByRole("img", { name: "Capa genérica de Sem Venda" })).toBeInTheDocument();
  expect(document.querySelector(".prazo-retirada.vencido")).not.toBeNull();
});

test("funcionário vê a dashboard e não vê carrinho nem pedidos", async () => {
  simularApi({
    "GET /dashboard": () => [
      200,
      {
        estoque: { titulosAtivos: 12, exemplaresParaReserva: 30, exemplaresParaCompra: 45, esgotadosParaReserva: 2 },
        reservas: { reservadas: 3, entregues: 1, devolvidas: 0, canceladas: 0 },
        materiasMaisReservadas: [{ nome: "TI", rotulo: "Ti", quantidade: 4 }],
        materiasMaisCompradas: [{ nome: "DIREITO", rotulo: "Direito", quantidade: 7 }],
      },
    ],
  });
  entrarComo("FUNCIONARIO");
  render(<App />);

  fireEvent.click(await screen.findByRole("button", { name: "Dashboard" }));

  expect(await screen.findByText("Títulos ativos")).toBeInTheDocument();
  expect(screen.getByText("45")).toBeInTheDocument();
  expect(screen.getByText("75% do total")).toBeInTheDocument();
  expect(screen.getByText("Direito")).toBeInTheDocument();
  expect(screen.queryByRole("button", { name: /^Carrinho/ })).not.toBeInTheDocument();
  expect(screen.queryByRole("button", { name: "Pedidos" })).not.toBeInTheDocument();
});
