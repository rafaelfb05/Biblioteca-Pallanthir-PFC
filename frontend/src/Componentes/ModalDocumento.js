import { useEffect, useRef, useState } from "react";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import { documentosApi } from "../services/api";

export const DOCUMENTOS = {
  termos: { titulo: "Termos de Uso", arquivo: "TERMOS_DE_USO.md", tipo: "TERMOS_DE_USO" },
  privacidade: {
    titulo: "Política de Privacidade",
    arquivo: "POLITICA_DE_PRIVACIDADE.md",
    tipo: "POLITICA_DE_PRIVACIDADE",
  },
};

const documentoPeloArquivo = (href) =>
  Object.keys(DOCUMENTOS).find((chave) =>
    href.endsWith(DOCUMENTOS[chave].arquivo)
  );

export default function ModalDocumento({ documento, aoFechar, aoTrocar }) {
  const [conteudo, setConteudo] = useState("");
  const [erro, setErro] = useState(null);
  const [progresso, setProgresso] = useState(0);
  const corpo = useRef(null);

  useEffect(() => {
    let ativo = true;
    setConteudo("");
    setErro(null);
    setProgresso(0);
    if (corpo.current) {
      corpo.current.scrollTop = 0;
    }

    documentosApi
      .obter(DOCUMENTOS[documento].tipo)
      .then((dados) => ativo && setConteudo(dados.conteudo))
      .catch(() => ativo && setErro("Não foi possível carregar o documento."));

    return () => {
      ativo = false;
    };
  }, [documento]);

  useEffect(() => {
    const elemento = corpo.current;
    if (!elemento || !conteudo) {
      return;
    }
    if (elemento.scrollHeight - elemento.clientHeight <= 0) {
      setProgresso(100);
    }
  }, [conteudo]);

  useEffect(() => {
    const aoTeclar = (evento) => {
      if (evento.key === "Escape") {
        aoFechar();
      }
    };
    document.addEventListener("keydown", aoTeclar);
    return () => document.removeEventListener("keydown", aoTeclar);
  }, [aoFechar]);

  const atualizarProgresso = () => {
    const elemento = corpo.current;
    if (!elemento) {
      return;
    }
    const total = elemento.scrollHeight - elemento.clientHeight;
    setProgresso(total > 0 ? Math.min(100, (elemento.scrollTop / total) * 100) : 100);
  };

  const voltarAoTopo = () => {
    if (corpo.current) {
      corpo.current.scrollTo({ top: 0, behavior: "smooth" });
    }
  };

  const renderizarLink = ({ href = "", children }) => {
    const outroDocumento = documentoPeloArquivo(href);

    if (outroDocumento) {
      return (
        <button
          type="button"
          className="link-button"
          onClick={() => aoTrocar(outroDocumento)}
        >
          {children}
        </button>
      );
    }

    return (
      <a href={href} target="_blank" rel="noreferrer">
        {children}
      </a>
    );
  };

  return (
    <div className="modal modal-documento" onClick={aoFechar}>
      <div
        className="documento-box"
        role="dialog"
        aria-modal="true"
        aria-labelledby="documento-titulo"
        onClick={(e) => e.stopPropagation()}
      >
        <header className="documento-topo">
          <div className="documento-topo-linha">
            <div className="documento-identificacao">
              <span className="documento-icone" aria-hidden="true">
                <svg
                  viewBox="0 0 24 24"
                  width="20"
                  height="20"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="1.8"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                >
                  <path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z" />
                  <path d="M14 3v5h5" />
                  <path d="M9 13h6M9 17h6" />
                </svg>
              </span>
              <div>
                <span className="documento-marca">Biblioteca Pallanthir</span>
                <h3 id="documento-titulo">{DOCUMENTOS[documento].titulo}</h3>
              </div>
            </div>
            <button
              type="button"
              className="documento-fechar"
              onClick={aoFechar}
              aria-label="Fechar documento"
            >
              ✕
            </button>
          </div>

          <div className="documento-abas" role="tablist">
            {Object.keys(DOCUMENTOS).map((chave) => (
              <button
                type="button"
                key={chave}
                role="tab"
                aria-selected={chave === documento}
                className={"documento-aba" + (chave === documento ? " ativa" : "")}
                onClick={() => chave !== documento && aoTrocar(chave)}
              >
                {DOCUMENTOS[chave].titulo}
              </button>
            ))}
          </div>

          <div className="documento-progresso" aria-hidden="true">
            <span style={{ width: progresso + "%" }} />
          </div>
        </header>

        <div className="documento-corpo" ref={corpo} onScroll={atualizarProgresso}>
          {erro && <p className="erro">{erro}</p>}
          {!erro && !conteudo && (
            <p className="documento-carregando">Carregando documento...</p>
          )}
          {conteudo && (
            <div className="documento-conteudo">
              <ReactMarkdown
                remarkPlugins={[remarkGfm]}
                components={{ a: renderizarLink }}
              >
                {conteudo}
              </ReactMarkdown>
            </div>
          )}
        </div>

        <footer className="documento-rodape">
          <span>
            {progresso >= 99 ? "Você chegou ao fim do documento." : "Role para ler o documento completo."}
          </span>
          <div className="documento-rodape-acoes">
            {progresso > 10 && (
              <button type="button" className="documento-topo-botao" onClick={voltarAoTopo}>
                ↑ Voltar ao topo
              </button>
            )}
            <button type="button" className="documento-ok" onClick={aoFechar}>
              Fechar
            </button>
          </div>
        </footer>
      </div>
    </div>
  );
}
