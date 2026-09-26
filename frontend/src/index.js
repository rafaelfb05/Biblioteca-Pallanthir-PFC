import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import App from './App';
import CopiarCodigo from './Componentes/CopiarCodigo';
import reportWebVitals from './reportWebVitals';

const codigoParaCopiar = new URLSearchParams(window.location.search).get('copiar');

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
  <React.StrictMode>
    {codigoParaCopiar && /^\d{6}$/.test(codigoParaCopiar) ? (
      <CopiarCodigo codigo={codigoParaCopiar} />
    ) : (
      <App />
    )}
  </React.StrictMode>
);

reportWebVitals();
