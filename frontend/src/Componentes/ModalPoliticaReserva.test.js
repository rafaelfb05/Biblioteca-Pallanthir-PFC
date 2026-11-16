import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import ModalPoliticaReserva from "./ModalPoliticaReserva";
import { versaoDosTermos } from "../services/termos";

jest.mock("react-markdown", () => {
  const React = require("react");
  return ({ children }) =>
    React.createElement(
      "div",
      null,
      String(children)
        .split("\n")
        .filter((linha) => linha.trim() && !linha.startsWith("#"))
        .map((linha) => React.createElement("p", { key: linha }, linha))
    );
});
jest.mock("remark-gfm", () => () => {});

const TEXTO = "# Política de Uso de Reservas\n\nTexto da política.";

beforeEach(() => {
  global.fetch = jest.fn(() =>
    Promise.resolve({ ok: true, text: () => Promise.resolve(TEXTO) })
  );
});

test("só confirma depois de marcar que está ciente e envia a versão da política", async () => {
  const aoConfirmar = jest.fn();
  render(
    <ModalPoliticaReserva
      livro={{ id: "1", titulo: "Clean Code" }}
      aoFechar={jest.fn()}
      aoConfirmar={aoConfirmar}
    />
  );

  expect(await screen.findByText("Texto da política.")).toBeInTheDocument();
  expect(screen.getByText(/Você está reservando/)).toHaveTextContent("Clean Code");
  const confirmar = screen.getByRole("button", { name: "Confirmar reserva" });
  expect(confirmar).toBeDisabled();

  await waitFor(() => expect(screen.getByRole("checkbox")).toBeEnabled());
  fireEvent.click(screen.getByRole("checkbox"));
  fireEvent.click(confirmar);

  expect(aoConfirmar).toHaveBeenCalledWith(versaoDosTermos(TEXTO));
});

test("cancelar fecha a política sem reservar", async () => {
  const aoFechar = jest.fn();
  const aoConfirmar = jest.fn();
  render(<ModalPoliticaReserva livro={{ id: "1", titulo: "Clean Code" }} aoFechar={aoFechar} aoConfirmar={aoConfirmar} />);

  await screen.findByText("Texto da política.");
  fireEvent.click(screen.getByRole("button", { name: "Cancelar" }));

  expect(aoFechar).toHaveBeenCalled();
  expect(aoConfirmar).not.toHaveBeenCalled();
});
