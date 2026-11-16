import { fireEvent, render, screen, within } from "@testing-library/react";
import EditorDocumentos from "./EditorDocumentos";

jest.mock("react-markdown", () => {
  const React = require("react");
  return ({ children }) => React.createElement("div", { "data-testid": "previa" }, String(children));
});
jest.mock("remark-gfm", () => () => {});

const DOCUMENTOS = [
  { tipo: "TERMOS_DE_USO", titulo: "Termos de Uso", conteudo: "# Termos\n\nTexto dos termos.", atualizadoEm: null, atualizadoPor: null },
  { tipo: "POLITICA_DE_PRIVACIDADE", titulo: "Política de Privacidade", conteudo: "# Privacidade", atualizadoEm: null, atualizadoPor: null },
  { tipo: "POLITICA_DE_RESERVAS", titulo: "Política de Uso de Reservas", conteudo: "# Reservas", atualizadoEm: null, atualizadoPor: null },
];

let chamadas;

beforeEach(() => {
  chamadas = [];
  global.fetch = jest.fn((url, opcoes = {}) => {
    const metodo = opcoes.method || "GET";
    const corpo = opcoes.body ? JSON.parse(opcoes.body) : null;
    chamadas.push({ metodo, url: String(url), corpo });
    if (String(url).includes("/versoes/")) {
      return Promise.resolve({
        ok: true,
        status: 200,
        text: () =>
          Promise.resolve(
            JSON.stringify({ id: "v1", tipo: "TERMOS_DE_USO", numero: 1, conteudo: "# Termos\n\nTexto antigo.", publicadoEm: null, publicadoPor: null, substituidaEm: "2026-11-16T09:00:00" })
          ),
      });
    }
    if (String(url).endsWith("/versoes")) {
      const lista = String(url).includes("TERMOS_DE_USO")
        ? [{ id: "v1", tipo: "TERMOS_DE_USO", numero: 1, conteudo: null, publicadoEm: null, publicadoPor: null, substituidaEm: "2026-11-16T09:00:00" }]
        : [];
      return Promise.resolve({ ok: true, status: 200, text: () => Promise.resolve(JSON.stringify(lista)) });
    }
    const dados =
      metodo === "PUT"
        ? {
            tipo: "POLITICA_DE_RESERVAS",
            titulo: "Política de Uso de Reservas",
            conteudo: corpo.conteudo,
            atualizadoEm: "2026-11-16T10:30:00",
            atualizadoPor: "Ana Gerente",
          }
        : DOCUMENTOS;
    return Promise.resolve({ ok: true, status: 200, text: () => Promise.resolve(JSON.stringify(dados)) });
  });
});

test("gerente edita a política de reservas, confirma e publica a nova versão", async () => {
  render(<EditorDocumentos />);

  expect(await screen.findByRole("tab", { name: "Termos de Uso" })).toHaveAttribute("aria-selected", "true");
  expect(screen.getByText(/todos os usuários precisarão ler e aceitar/)).toBeInTheDocument();

  fireEvent.click(screen.getByRole("tab", { name: "Política de Uso de Reservas" }));
  const texto = screen.getByRole("textbox", { name: "Conteúdo do documento" });
  expect(texto).toHaveValue("# Reservas");
  expect(screen.getByRole("button", { name: "Salvar documento" })).toBeDisabled();

  fireEvent.change(texto, { target: { value: "# Reservas\n\nPrazo de 5 dias." } });
  fireEvent.click(screen.getByRole("button", { name: "Pré-visualizar" }));
  expect(screen.getByTestId("previa")).toHaveTextContent("Prazo de 5 dias.");

  fireEvent.click(screen.getByRole("button", { name: "Salvar documento" }));
  const dialogo = await screen.findByRole("alertdialog");
  expect(dialogo).toHaveTextContent("próximas reservas");
  fireEvent.click(within(dialogo).getByRole("button", { name: "Publicar" }));

  expect(await screen.findByText("Política de Uso de Reservas salvo com sucesso.")).toBeInTheDocument();
  const envio = chamadas.find((c) => c.metodo === "PUT");
  expect(envio.url).toMatch(/\/documentos\/POLITICA_DE_RESERVAS$/);
  expect(envio.corpo).toEqual({ conteudo: "# Reservas\n\nPrazo de 5 dias." });
  expect(screen.getByText(/Última alteração em 16\/11\/2026.*por Ana Gerente/)).toBeInTheDocument();
});

test("descartar volta ao texto salvo sem chamar a API", async () => {
  render(<EditorDocumentos />);

  const texto = await screen.findByRole("textbox", { name: "Conteúdo do documento" });
  fireEvent.change(texto, { target: { value: "Rascunho" } });
  expect(screen.getByTitle("Alterações não salvas")).toBeInTheDocument();

  fireEvent.click(screen.getByRole("button", { name: "Descartar alterações" }));
  expect(texto).toHaveValue("# Termos\n\nTexto dos termos.");
  expect(chamadas.filter((c) => c.metodo === "PUT")).toHaveLength(0);
});

test("versões anteriores podem ser vistas e copiadas para o editor", async () => {
  render(<EditorDocumentos />);

  fireEvent.click(await screen.findByRole("button", { name: "Ver versão 1" }));
  const dialogo = await screen.findByRole("dialog");
  expect(dialogo).toHaveTextContent("Versão 1 · Termos de Uso");
  expect(dialogo).toHaveTextContent("Versão original do sistema");
  expect(within(dialogo).getByTestId("previa")).toHaveTextContent("Texto antigo.");

  fireEvent.click(within(dialogo).getByRole("button", { name: "Usar esta versão no editor" }));
  expect(screen.getByRole("textbox", { name: "Conteúdo do documento" })).toHaveValue("# Termos\n\nTexto antigo.");
  expect(screen.getByText(/O texto da versão 1 foi copiado para o editor/)).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Salvar documento" })).toBeEnabled();
  expect(chamadas.filter((c) => c.metodo === "PUT")).toHaveLength(0);
});

