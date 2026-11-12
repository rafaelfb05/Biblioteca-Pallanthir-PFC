import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import App from "./App";

jest.mock("./Componentes/ModalDocumento", () => () => null);

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
