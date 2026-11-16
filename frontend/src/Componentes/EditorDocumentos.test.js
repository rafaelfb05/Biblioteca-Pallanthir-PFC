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
