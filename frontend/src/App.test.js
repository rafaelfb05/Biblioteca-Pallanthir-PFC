import { render, screen } from '@testing-library/react';
import App from './App';

jest.mock('./Componentes/ModalDocumento', () => () => null);
jest.mock('./Componentes/EditorDocumentos', () => () => null);
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

test('exibe o nome da biblioteca', () => {
  render(<App />);
  expect(screen.getAllByText(/Pallanthir/i).length).toBeGreaterThan(0);
});
