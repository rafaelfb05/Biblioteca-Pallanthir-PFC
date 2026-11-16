import { useEffect } from "react";

export function IconeSino({ preenchido = false, tamanho = 18 }) {
  return (
    <svg
      viewBox="0 0 24 24"
      width={tamanho}
      height={tamanho}
      fill={preenchido ? "currentColor" : "none"}
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9" />
      <path d="M13.73 21a2 2 0 0 1-3.46 0" />
    </svg>
  );
}

const ICONES = {
  perigo: "✕",
  alerta: "!",
  sino: <IconeSino tamanho={24} />,
};

export default function DialogoConfirmacao({
  tom = "alerta",
  titulo,
  children,
  alerta,
  textoConfirmar,
  textoEnviando = "Aguarde...",
  textoCancelar = "Cancelar",
  enviando = false,
  aoConfirmar,
  aoCancelar,
}) {
  useEffect(() => {
    const aoTeclar = (evento) => {
      if (evento.key === "Escape" && !enviando) {
        aoCancelar();
      }
    };
    document.addEventListener("keydown", aoTeclar);
    return () => document.removeEventListener("keydown", aoTeclar);
  }, [aoCancelar, enviando]);

  return (
    <div className="modal" onClick={() => !enviando && aoCancelar()}>
      <div
        className={"modal-box dialogo " + tom}
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="dialogo-confirmacao-titulo"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="dialogo-icone" aria-hidden="true">
          {ICONES[tom] || ICONES.alerta}
        </div>
        <h3 id="dialogo-confirmacao-titulo">{titulo}</h3>
        {children}
        {alerta && <p className="dialogo-alerta">{alerta}</p>}
        <div className="dialogo-acoes">
          <button
            type="button"
            className="dialogo-secundario"
            onClick={aoCancelar}
            disabled={enviando}
          >
            {textoCancelar}
          </button>
          <button
            type="button"
            className="dialogo-principal"
            onClick={aoConfirmar}
            disabled={enviando}
          >
            {enviando ? textoEnviando : textoConfirmar}
          </button>
        </div>
      </div>
    </div>
  );
}
