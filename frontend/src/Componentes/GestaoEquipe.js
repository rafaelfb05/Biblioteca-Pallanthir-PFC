import { useCallback, useEffect, useState } from "react";
import { usuariosApi } from "../services/api";

const ROTULO_PERFIL = { GERENTE: "Gerente", FUNCIONARIO: "Funcionário" };

const CONFIRMACOES = {
  promover: {
    titulo: "Tornar gerente?",
    texto: (nome) =>
      nome + " poderá consultar os logs, ver o dashboard de vendas, editar os termos e gerenciar a equipe.",
    botao: "Tornar gerente",
    enviando: "Salvando...",
  },
  rebaixar: {
    titulo: "Tornar funcionário?",
    texto: (nome) =>
      nome + " deixará de ter acesso aos logs, ao dashboard de vendas, aos termos e à gestão da equipe.",
    botao: "Tornar funcionário",
    enviando: "Salvando...",
  },
  excluir: {
    titulo: "Excluir conta do funcionário?",
    texto: (nome) =>
      "A conta de " + nome + " será desativada e os dados pessoais serão anonimizados. O funcionário perde o acesso ao sistema.",
    botao: "Excluir conta",
    enviando: "Excluindo...",
  },
};

const iniciais = (nome) =>
  String(nome || "?")
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((parte) => parte.charAt(0).toUpperCase())
    .join("");

const mensagem = (erro, padrao) => (erro && erro.status && erro.message ? erro.message : padrao);

export default function GestaoEquipe({ usuarioAtualId }) {
  const [equipe, setEquipe] = useState([]);
  const [busca, setBusca] = useState("");
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState(null);
  const [aviso, setAviso] = useState(null);
  const [confirmacao, setConfirmacao] = useState(null);
  const [enviando, setEnviando] = useState(false);

  const carregar = useCallback(async () => {
    try {
      setCarregando(true);
      setErro(null);
      const usuarios = (await usuariosApi.listar()) || [];
      setEquipe(
        usuarios
          .filter((u) => u.tipo === "FUNCIONARIO" || u.tipo === "GERENTE")
          .sort((a, b) =>
            a.tipo === b.tipo ? a.nome.localeCompare(b.nome, "pt-BR") : a.tipo === "GERENTE" ? -1 : 1
          )
      );
    } catch (e) {
      setErro(mensagem(e, "Não foi possível carregar a equipe."));
    } finally {
      setCarregando(false);
    }
  }, []);

  useEffect(() => {
    carregar();
  }, [carregar]);

  const confirmar = async () => {
    const { acao, membro } = confirmacao;
    try {
      setEnviando(true);
      setErro(null);
      if (acao === "excluir") {
        await usuariosApi.excluirFuncionario(membro.id);
        setAviso("A conta de " + membro.nome + " foi excluída.");
      } else {
        const tipo = acao === "promover" ? "GERENTE" : "FUNCIONARIO";
        await usuariosApi.alterarPerfil(membro.id, tipo);
        setAviso(membro.nome + " agora é " + ROTULO_PERFIL[tipo].toLowerCase() + ".");
      }
      setConfirmacao(null);
      await carregar();
    } catch (e) {
      setConfirmacao(null);
      setErro(mensagem(e, "Não foi possível concluir a ação."));
    } finally {
      setEnviando(false);
    }
  };

  const termo = busca.trim().toLowerCase();
  const filtrados = equipe.filter(
    (membro) =>
      !termo || membro.nome.toLowerCase().includes(termo)
  );
  const gerentes = equipe.filter((membro) => membro.tipo === "GERENTE").length;

  return (
    <div className="gestao-equipe">
      <div className="equipe-topo">
        <input
          type="search"
          className="equipe-busca"
          placeholder="Buscar por nome"
          aria-label="Buscar na equipe"
          value={busca}
          onChange={(e) => setBusca(e.target.value)}
        />
        <span className="equipe-contagem">
          {gerentes} gerente(s) · {equipe.length - gerentes} funcionário(s)
        </span>
      </div>

      {aviso && (
        <p className="aviso" onAnimationEnd={() => setAviso(null)}>
          {aviso}
        </p>
      )}
      {erro && <p className="erro">{erro}</p>}
      {carregando && equipe.length === 0 && <p>Carregando equipe...</p>}
      {!carregando && filtrados.length === 0 && (
        <p className="dashboard-vazio">Ninguém encontrado.</p>
      )}

      <ul className="equipe-lista">
        {filtrados.map((membro) => {
          const ehVoce = membro.id === usuarioAtualId;
          return (
            <li key={membro.id} className="equipe-card">
              <span className={"equipe-avatar " + membro.tipo.toLowerCase()} aria-hidden="true">
                {iniciais(membro.nome)}
              </span>
              <div className="equipe-info">
                <strong>
                  {membro.nome}
                  {ehVoce && <span className="equipe-voce">você</span>}
                </strong>
                <span>{membro.email}</span>
              </div>
              <span className={"equipe-perfil " + membro.tipo.toLowerCase()}>
                {ROTULO_PERFIL[membro.tipo]}
              </span>
              <div className="equipe-acoes">
                {!ehVoce && membro.tipo === "FUNCIONARIO" && (
                  <>
                    <button
                      type="button"
                      className="equipe-botao"
                      onClick={() => setConfirmacao({ acao: "promover", membro })}
                    >
                      Tornar gerente
                    </button>
                    <button
                      type="button"
                      className="equipe-botao perigo"
                      onClick={() => setConfirmacao({ acao: "excluir", membro })}
                    >
                      Excluir conta
                    </button>
                  </>
                )}
                {!ehVoce && membro.tipo === "GERENTE" && (
                  <button
                    type="button"
                    className="equipe-botao"
                    onClick={() => setConfirmacao({ acao: "rebaixar", membro })}
                  >
                    Tornar funcionário
                  </button>
                )}
              </div>
            </li>
          );
        })}
      </ul>

      {confirmacao && (
        <div className="modal" onClick={() => !enviando && setConfirmacao(null)}>
          <div
            className={"modal-box dialogo" + (confirmacao.acao === "excluir" ? " perigo" : " bloqueado")}
            role="alertdialog"
            aria-modal="true"
            aria-labelledby="equipe-dialogo-titulo"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="dialogo-icone" aria-hidden="true">
              {confirmacao.acao === "excluir" ? "✕" : "!"}
            </div>
            <h3 id="equipe-dialogo-titulo">{CONFIRMACOES[confirmacao.acao].titulo}</h3>
            <p>{CONFIRMACOES[confirmacao.acao].texto(confirmacao.membro.nome)}</p>
            {confirmacao.acao === "excluir" && (
              <p className="dialogo-alerta">Esta ação não pode ser desfeita.</p>
            )}
            <div className="dialogo-acoes">
              <button
                type="button"
                className="dialogo-secundario"
                onClick={() => setConfirmacao(null)}
                disabled={enviando}
              >
                Cancelar
              </button>
              <button type="button" className="dialogo-principal" onClick={confirmar} disabled={enviando}>
                {enviando ? CONFIRMACOES[confirmacao.acao].enviando : CONFIRMACOES[confirmacao.acao].botao}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
