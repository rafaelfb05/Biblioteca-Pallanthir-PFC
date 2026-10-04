import { useEffect, useState } from "react";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";

export const DOCUMENTOS = {
  termos: { titulo: "Termos de Uso", arquivo: "TERMOS_DE_USO.md" },
  privacidade: {
    titulo: "Política de Privacidade",
    arquivo: "POLITICA_DE_PRIVACIDADE.md",
  },
};

const documentoPeloArquivo = (href) =>
  Object.keys(DOCUMENTOS).find((chave) =>
    href.endsWith(DOCUMENTOS[chave].arquivo)
  );

export default function ModalDocumento({ documento, aoFechar, aoTrocar }) {
  const [conteudo, setConteudo] = useState("");
  const [erro, setErro] = useState(null);

  useEffect(() => {
    let ativo = true;
    setConteudo("");
    setErro(null);

    fetch(process.env.PUBLIC_URL + "/docs/" + DOCUMENTOS[documento].arquivo)
      .then((resposta) => {
        if (!resposta.ok) {
          throw new Error();
        }
        return resposta.text();
      })
      .then((texto) => ativo && setConteudo(texto))
      .catch(() => ativo && setErro("Não foi possível carregar o documento."));

    return () => {
      ativo = false;
    };
  }, [documento]);

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
      <div className="modal-box" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3>{DOCUMENTOS[documento].titulo}</h3>
          <button type="button" onClick={aoFechar}>
            ✕
          </button>
        </div>

        {erro && <p className="erro">{erro}</p>}
        {!erro && !conteudo && <p className="modal-sub">Carregando...</p>}
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

        <div className="modal-actions">
          <button type="button" onClick={aoFechar}>
            Fechar
          </button>
        </div>
      </div>
    </div>
  );
}
