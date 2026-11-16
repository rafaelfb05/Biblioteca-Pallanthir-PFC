import { useCallback, useEffect, useState } from "react";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import { documentosApi } from "../services/api";

const AVISO_POR_TIPO = {
  TERMOS_DE_USO:
    "Ao salvar, todos os usuários precisarão ler e aceitar os Termos de Uso e a Política de Privacidade novamente no próximo acesso.",
  POLITICA_DE_PRIVACIDADE:
    "Ao salvar, todos os usuários precisarão ler e aceitar os Termos de Uso e a Política de Privacidade novamente no próximo acesso.",
  POLITICA_DE_RESERVAS:
    "Ao salvar, a nova versão passa a ser exibida e aceita nas próximas reservas. As reservas já feitas continuam com a versão aceita na época.",
};

const TAMANHO_MAXIMO = 100000;

const formatarDataHora = (valor) =>
  new Date(valor).toLocaleString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });

const mensagem = (erro, padrao) => (erro && erro.status && erro.message ? erro.message : padrao);

export default function EditorDocumentos({ aoSalvar }) {
  const [documentos, setDocumentos] = useState([]);
  const [tipo, setTipo] = useState(null);
  const [rascunhos, setRascunhos] = useState({});
  const [modo, setModo] = useState("editar");
  const [carregando, setCarregando] = useState(false);
  const [salvando, setSalvando] = useState(false);
  const [confirmando, setConfirmando] = useState(false);
  const [erro, setErro] = useState(null);
  const [aviso, setAviso] = useState(null);

  const carregar = useCallback(async () => {
    try {
      setCarregando(true);
      setErro(null);
      const lista = (await documentosApi.listar()) || [];
      setDocumentos(lista);
      setRascunhos(Object.fromEntries(lista.map((doc) => [doc.tipo, doc.conteudo])));
      setTipo((atual) => atual || (lista[0] && lista[0].tipo));
    } catch (e) {
      setErro(mensagem(e, "Não foi possível carregar os documentos."));
    } finally {
      setCarregando(false);
    }
  }, []);

  useEffect(() => {
    carregar();
  }, [carregar]);

  const documento = documentos.find((doc) => doc.tipo === tipo);
  const rascunho = documento ? rascunhos[tipo] ?? "" : "";
  const alterado = (doc) => doc && (rascunhos[doc.tipo] ?? "").trim() !== doc.conteudo.trim();

  const trocarDocumento = (novo) => {
    setTipo(novo);
    setModo("editar");
    setAviso(null);
    setErro(null);
  };

  const salvar = async () => {
    try {
      setSalvando(true);
      setErro(null);
      const salvo = await documentosApi.atualizar(tipo, rascunho);
      setDocumentos((lista) => lista.map((doc) => (doc.tipo === salvo.tipo ? salvo : doc)));
      setRascunhos((atuais) => ({ ...atuais, [salvo.tipo]: salvo.conteudo }));
      setAviso(salvo.titulo + " salvo com sucesso.");
      setConfirmando(false);
      if (aoSalvar) {
        aoSalvar(salvo.tipo);
      }
    } catch (e) {
      setConfirmando(false);
      setErro(mensagem(e, "Não foi possível salvar o documento."));
    } finally {
      setSalvando(false);
    }
  };

  return (
    <div className="editor-documentos">
      {carregando && documentos.length === 0 && <p>Carregando documentos...</p>}
      {erro && <p className="erro">{erro}</p>}

      {documentos.length > 0 && (
        <div className="gestao-abas" role="tablist" aria-label="Documento">
          {documentos.map((doc) => (
            <button
              type="button"
              role="tab"
              key={doc.tipo}
              aria-selected={doc.tipo === tipo}
              className={"gestao-aba" + (doc.tipo === tipo ? " ativa" : "")}
              onClick={() => trocarDocumento(doc.tipo)}
            >
              {doc.titulo}
              {alterado(doc) && (
                <span className="editor-alterado" title="Alterações não salvas">
                  •
                </span>
              )}
            </button>
          ))}
        </div>
      )}

      {documento && (
        <div className="editor-painel">
          <div className="editor-cabecalho">
            <div>
              <h3>{documento.titulo}</h3>
              <span className="editor-atualizacao">
                {documento.atualizadoEm
                  ? "Última alteração em " +
                    formatarDataHora(documento.atualizadoEm) +
                    (documento.atualizadoPor ? " por " + documento.atualizadoPor : "")
                  : "Versão original do sistema"}
              </span>
            </div>
            <div className="editor-modos" role="group" aria-label="Modo de exibição">
              <button
                type="button"
                className={"editor-modo" + (modo === "editar" ? " ativo" : "")}
                aria-pressed={modo === "editar"}
                onClick={() => setModo("editar")}
              >
                Editar
              </button>
              <button
                type="button"
                className={"editor-modo" + (modo === "visualizar" ? " ativo" : "")}
                aria-pressed={modo === "visualizar"}
                onClick={() => setModo("visualizar")}
              >
                Pré-visualizar
              </button>
            </div>
          </div>

          <p className="editor-aviso">{AVISO_POR_TIPO[documento.tipo]}</p>
          {aviso && (
        <p className="aviso" onAnimationEnd={() => setAviso(null)}>
          {aviso}
        </p>
      )}

          {modo === "editar" ? (
            <>
              <textarea
                className="editor-texto"
                aria-label="Conteúdo do documento"
                value={rascunho}
                maxLength={TAMANHO_MAXIMO}
                spellCheck
                onChange={(e) => {
                  setAviso(null);
                  setRascunhos({ ...rascunhos, [tipo]: e.target.value });
                }}
              />
              <span className="editor-dica">
                Use Markdown: # para títulos, **texto** para negrito e - para listas ·{" "}
                {rascunho.length.toLocaleString("pt-BR")} caracteres
              </span>
            </>
          ) : (
            <div className="editor-previa documento-conteudo">
              <ReactMarkdown remarkPlugins={[remarkGfm]}>{rascunho}</ReactMarkdown>
            </div>
          )}

          <div className="editor-acoes">
            <button
              type="button"
              className="dialogo-secundario"
              disabled={!alterado(documento) || salvando}
              onClick={() => setRascunhos({ ...rascunhos, [tipo]: documento.conteudo })}
            >
              Descartar alterações
            </button>
            <button
              type="button"
              className="dialogo-principal"
              disabled={!alterado(documento) || !rascunho.trim() || salvando}
              onClick={() => setConfirmando(true)}
            >
              Salvar documento
            </button>
          </div>
        </div>
      )}

      {confirmando && documento && (
        <div className="modal" onClick={() => !salvando && setConfirmando(false)}>
          <div
            className="modal-box dialogo bloqueado"
            role="alertdialog"
            aria-modal="true"
            aria-labelledby="editor-dialogo-titulo"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="dialogo-icone" aria-hidden="true">
              !
            </div>
            <h3 id="editor-dialogo-titulo">Publicar nova versão?</h3>
            <p>{AVISO_POR_TIPO[documento.tipo]}</p>
            <div className="dialogo-acoes">
              <button
                type="button"
                className="dialogo-secundario"
                onClick={() => setConfirmando(false)}
                disabled={salvando}
              >
                Cancelar
              </button>
              <button type="button" className="dialogo-principal" onClick={salvar} disabled={salvando}>
                {salvando ? "Salvando..." : "Publicar"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
