import { render, screen } from '@testing-library/react';
import App from './App';

test('exibe o nome da biblioteca', () => {
  render(<App />);
  expect(screen.getAllByText(/Pallanthir/i).length).toBeGreaterThan(0);
});
