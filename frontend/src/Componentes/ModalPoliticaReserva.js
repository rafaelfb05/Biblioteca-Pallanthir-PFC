import { useEffect, useRef, useState } from "react";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import { carregarPoliticaDeReservas } from "../services/termos";

export default function ModalPoliticaReserva({
  livro,
  aoFechar,
  aoConfirmar,
  enviando = false,
}) {
  const [politica, setPolitica] = useState(null);
  const [erro, setErro] = useState(null);
  const [progresso, setProgresso] = useState(0);
  const [leuTudo, setLeuTudo] = useState(false);
  const [ciente, setCiente] = useState(false);
  const corpo = useRef(null);

  useEffect(() => {
    let ativo = true;
    carregarPoliticaDeReservas()
      .then((dados) => ativo && setPolitica(dados))
      .catch(() => ativo && setErro("Não foi possível carregar a política de uso."));
    return () => {
      ativo = false;
    };
  }, []);

  useEffect(() => {
    const aoTeclar = (evento) => {
      if (evento.key === "Escape" && !enviando) {
        aoFechar();
      }
    };
    document.addEventListener("keydown", aoTeclar);
    return () => document.removeEventListener("keydown", aoTeclar);
  }, [aoFechar, enviando]);

  useEffect(() => {
    const elemento = corpo.current;
    if (elemento && politica && elemento.scrollHeight - elemento.clientHeight <= 0) {
      setProgresso(100);
      setLeuTudo(true);
    }
  }, [politica]);

  const atualizarProgresso = () => {
    const elemento = corpo.current;
    if (!elemento) {
      return;
    }
    const total = elemento.scrollHeight - elemento.clientHeight;
    const percentual = total > 0 ? Math.min(100, (elemento.scrollTop / total) * 100) : 100;
    setProgresso(percentual);
    if (total <= 0 || total - elemento.scrollTop <= 8) {
      setLeuTudo(true);
    }
  };

  const podeConfirmar = Boolean(politica) && leuTudo && ciente && !enviando;

  const confirmar = () => {
    if (podeConfirmar) {
      aoConfirmar(politica.versao);
    }
  };

  return (
    <div className="modal modal-documento" onClick={() => !enviando && aoFechar()}>
      <div
        className="documento-box"
        role="dialog"
        aria-modal="true"
        aria-labelledby="politica-reserva-titulo"
        onClick={(e) => e.stopPropagation()}
      >
        <header className="documento-topo">
          <div className="documento-topo-linha">
            <div className="documento-identificacao">
              <div>
                <span className="documento-marca">Biblioteca Pallanthir</span>
                <h3 id="politica-reserva-titulo">Política de Uso de Reservas</h3>
              </div>
            </div>
            <button
              type="button"
              className="documento-fechar"
              onClick={aoFechar}
              disabled={enviando}
              aria-label="Fechar"
            >
              ✕
            </button>
          </div>

          {livro && (
            <p className="politica-aviso">
              Você está reservando <strong>{livro.titulo || livro.title}</strong>. Se o
              livro for danificado ou perdido enquanto estiver com você, será necessário
              pagar o valor do exemplar.
            </p>
          )}

          <div className="documento-progresso" aria-hidden="true">
            <span style={{ width: progresso + "%" }} />
          </div>
        </header>

        <div className="documento-corpo" ref={corpo} onScroll={atualizarProgresso}>
          {erro && <p className="erro">{erro}</p>}
          {!erro && !politica && (
            <p className="documento-carregando">Carregando política...</p>
          )}
          {politica && (
            <div className="documento-conteudo">
              <ReactMarkdown remarkPlugins={[remarkGfm]}>{politica.texto}</ReactMarkdown>
            </div>
          )}
        </div>

        <footer className="documento-rodape politica-rodape">
          <label className={"politica-ciente" + (leuTudo ? "" : " desabilitado")}>
            <input
              type="checkbox"
              checked={ciente}
              disabled={!leuTudo || enviando}
              onChange={(e) => setCiente(e.target.checked)}
            />
            <span>
              {leuTudo
                ? "Li e estou ciente de que, se o livro for danificado ou perdido, terei que pagar por ele."
                : "Role até o fim da política para poder aceitar."}
            </span>
          </label>

          <div className="documento-rodape-acoes">
            <button
              type="button"
              className="documento-topo-botao"
              onClick={aoFechar}
              disabled={enviando}
            >
              Cancelar
            </button>
            <button
              type="button"
              className="documento-ok"
              onClick={confirmar}
              disabled={!podeConfirmar}
            >
              {enviando ? "Reservando..." : "Confirmar reserva"}
            </button>
          </div>
        </footer>
      </div>
    </div>
  );
}
