import { useEffect, useState } from "react";
import "../App.css";

export default function CopiarCodigo({ codigo }) {
  const [copiado, setCopiado] = useState(false);

  const copiar = async () => {
    try {
      await navigator.clipboard.writeText(codigo);
      setCopiado(true);
    } catch (e) {
      setCopiado(false);
    }
  };

  useEffect(() => {
    navigator.clipboard
      .writeText(codigo)
      .then(() => setCopiado(true))
      .catch(() => setCopiado(false));
  }, [codigo]);

  return (
    <div className="app pagina-copiar-codigo">
      <div className="copiar-codigo-card">
        <span className="logo">Pallanthir</span>
        <small>Seu código de verificação</small>
        <strong className="copiar-codigo-valor">{codigo}</strong>
        <button type="button" className="reservar-button" onClick={copiar}>
          {copiado ? "Código copiado!" : "Copiar código"}
        </button>
        <p>
          {copiado
            ? "Volte para a aba da Biblioteca Pallanthir e cole o código. Você já pode fechar esta página."
            : "Clique no botão para copiar o código e depois cole na aba da Biblioteca Pallanthir."}
        </p>
      </div>
    </div>
  );
}
