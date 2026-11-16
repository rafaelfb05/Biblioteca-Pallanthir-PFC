import { useCallback, useEffect, useState } from "react";
import { dashboardApi } from "../services/api";

const PERIODOS = [
  { chave: "7", rotulo: "7 dias", dias: 7 },
  { chave: "30", rotulo: "30 dias", dias: 30 },
  { chave: "90", rotulo: "90 dias", dias: 90 },
  { chave: "365", rotulo: "12 meses", dias: 365 },
  { chave: "tudo", rotulo: "Tudo", dias: null },
];

const ROTULO_STATUS = {
  AGUARDANDO_PAGAMENTO: "Aguardando pagamento",
  APROVADO: "Pago · aguardando retirada",
  ENTREGUE: "Retirado",
  EXPIRADO: "Expirado",
  CANCELADO: "Cancelado",
  REEMBOLSADO: "Reembolsado",
};

const MESES = ["jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez"];

const moeda = (valor) =>
  Number(valor || 0).toLocaleString("pt-BR", { style: "currency", currency: "BRL" });

const numero = (valor) => Number(valor || 0).toLocaleString("pt-BR");

const doisDigitos = (valor) => String(valor).padStart(2, "0");

const dataIso = (data) =>
  data.getFullYear() + "-" + doisDigitos(data.getMonth() + 1) + "-" + doisDigitos(data.getDate());

const intervaloDe = (dias) => {
  if (!dias) {
    return {};
  }
  const inicio = new Date();
  inicio.setDate(inicio.getDate() - (dias - 1));
  return { de: dataIso(inicio), ate: dataIso(new Date()) };
};

const rotuloDoMes = (mes) => {
  const [ano, numeroDoMes] = String(mes).split("-");
  return MESES[Number(numeroDoMes) - 1] + "/" + String(ano).slice(2);
};

function Ranking({ titulo, descricao, itens }) {
  const maior = itens.reduce((max, item) => Math.max(max, item.quantidade), 0);
  return (
    <div className="dashboard-card">
      <h3>{titulo}</h3>
      <p className="dashboard-card-sub">{descricao}</p>
      {itens.length === 0 ? (
        <p className="dashboard-vazio">Nenhuma venda no período.</p>
      ) : (
        <ol className="vendas-ranking">
          {itens.map((item, posicao) => (
            <li key={item.nome}>
              <span className="vendas-ranking-posicao">{posicao + 1}</span>
              <div className="vendas-ranking-info">
                <div className="vendas-ranking-linha">
                  <span className="vendas-ranking-nome" title={item.rotulo}>
                    {item.rotulo}
                  </span>
                  <strong>{numero(item.quantidade)} un.</strong>
                </div>
                <span className="dashboard-barra-trilho" aria-hidden="true">
                  <span
                    className="dashboard-barra"
                    style={{ width: maior > 0 ? Math.max((item.quantidade / maior) * 100, 2) + "%" : "0%" }}
                  />
                </span>
                <small>{moeda(item.receita)} em vendas</small>
              </div>
            </li>
          ))}
        </ol>
      )}
    </div>
  );
}

export default function PainelVendas() {
  const [periodo, setPeriodo] = useState("30");
  const [personalizado, setPersonalizado] = useState({ de: "", ate: "" });
  const [dados, setDados] = useState(null);
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState(null);

  const carregar = useCallback(async (filtros) => {
    try {
      setCarregando(true);
      setErro(null);
      setDados(await dashboardApi.vendas(filtros));
    } catch (e) {
      setErro(e && e.status && e.message ? e.message : "Não foi possível carregar as vendas.");
    } finally {
      setCarregando(false);
    }
  }, []);

  useEffect(() => {
    carregar(intervaloDe(30));
  }, [carregar]);

  const escolherPeriodo = (opcao) => {
    setPeriodo(opcao.chave);
    setPersonalizado({ de: "", ate: "" });
    carregar(intervaloDe(opcao.dias));
  };

  const aplicarPersonalizado = (evento) => {
    evento.preventDefault();
    if (!personalizado.de || !personalizado.ate) {
      setErro("Informe a data inicial e a data final.");
      return;
    }
    if (personalizado.de > personalizado.ate) {
      setErro("A data inicial não pode ser posterior à data final.");
      return;
    }
    setPeriodo("personalizado");
    carregar(personalizado);
  };

  const filtrosAtuais = () =>
    periodo === "personalizado"
      ? personalizado
      : intervaloDe((PERIODOS.find((opcao) => opcao.chave === periodo) || {}).dias);

  const resumo = dados ? dados.resumo : null;
  const meses = dados ? (dados.vendasPorMes || []).slice(-12) : [];
  const maiorMes = meses.reduce((max, mes) => Math.max(max, Number(mes.faturamento)), 0);
  const totalPedidos = dados
    ? Object.values(dados.pedidosPorStatus || {}).reduce((soma, valor) => soma + valor, 0)
    : 0;

  return (
    <div className="painel-vendas">
      <div className="vendas-filtros">
        <div className="gestao-abas" role="tablist" aria-label="Período">
          {PERIODOS.map((opcao) => (
            <button
              type="button"
              role="tab"
              key={opcao.chave}
              aria-selected={periodo === opcao.chave}
              className={"gestao-aba" + (periodo === opcao.chave ? " ativa" : "")}
              onClick={() => escolherPeriodo(opcao)}
            >
              {opcao.rotulo}
            </button>
          ))}
        </div>

        <form className="vendas-periodo" onSubmit={aplicarPersonalizado}>
          <label>
            De
            <input
              type="date"
              value={personalizado.de}
              onChange={(e) => setPersonalizado({ ...personalizado, de: e.target.value })}
            />
          </label>
          <label>
            Até
            <input
              type="date"
              value={personalizado.ate}
              onChange={(e) => setPersonalizado({ ...personalizado, ate: e.target.value })}
            />
          </label>
          <button type="submit" className="vendas-aplicar">
            Aplicar
          </button>
        </form>

        <button
          type="button"
          className="favoritar-button"
          onClick={() => carregar(filtrosAtuais())}
          disabled={carregando}
        >
          {carregando ? "Carregando..." : "Atualizar"}
        </button>
      </div>

      {erro && <p className="erro">{erro}</p>}
      {!dados && carregando && <p>Carregando vendas...</p>}

      {resumo && (
        <>
          <h3 className="dashboard-titulo">Resumo do período</h3>
          <div className="dashboard-numeros">
            <div className="dashboard-numero vendas-destaque">
              <small>Faturamento</small>
              <strong>{moeda(resumo.faturamento)}</strong>
              <small>Pedidos pagos, sem reembolsos</small>
            </div>
            <div className="dashboard-numero">
              <small>Pedidos pagos</small>
              <strong>{numero(resumo.pedidosPagos)}</strong>
            </div>
            <div className="dashboard-numero">
              <small>Ticket médio</small>
              <strong>{moeda(resumo.ticketMedio)}</strong>
            </div>
            <div className="dashboard-numero">
              <small>Exemplares vendidos</small>
              <strong>{numero(resumo.exemplaresVendidos)}</strong>
            </div>
          </div>

          <div className="dashboard-numeros vendas-secundarios">
            <div className="dashboard-numero">
              <small>Aguardando retirada</small>
              <strong>{numero(resumo.aguardandoRetirada)}</strong>
            </div>
            <div className="dashboard-numero">
              <small>Retirados</small>
              <strong>{numero(resumo.entregues)}</strong>
            </div>
            <div className="dashboard-numero">
              <small>Reembolsos</small>
              <strong>{moeda(resumo.valorReembolsado)}</strong>
              <small>{numero(resumo.reembolsados)} pedido(s)</small>
            </div>
            <div className="dashboard-numero">
              <small>Multas recebidas</small>
              <strong>{moeda(dados.multas.valorRecebido)}</strong>
              <small>
                {numero(dados.multas.recebidas)} paga(s) · {moeda(dados.multas.valorPendente)} pendente(s)
              </small>
            </div>
          </div>

          <div className="dashboard-card vendas-grafico">
            <h3>Faturamento por mês</h3>
            <p className="dashboard-card-sub">Valor dos pedidos pagos em cada mês do período.</p>
            {meses.length === 0 ? (
              <p className="dashboard-vazio">Nenhuma venda no período.</p>
            ) : (
              <ol className="vendas-meses">
                {meses.map((mes) => (
                  <li key={mes.mes} title={rotuloDoMes(mes.mes) + ": " + moeda(mes.faturamento)}>
                    <span className="vendas-mes-valor">{moeda(mes.faturamento)}</span>
                    <span className="vendas-mes-trilho" aria-hidden="true">
                      <span
                        className="vendas-mes-barra"
                        style={{
                          height:
                            maiorMes > 0 ? Math.max((Number(mes.faturamento) / maiorMes) * 100, 3) + "%" : "0%",
                        }}
                      />
                    </span>
                    <span className="vendas-mes-rotulo">{rotuloDoMes(mes.mes)}</span>
                    <small>{numero(mes.pedidos)} pedido(s)</small>
                  </li>
                ))}
              </ol>
            )}
          </div>

          <div className="dashboard-graficos">
            <Ranking
              titulo="Livros mais vendidos"
              descricao="Os 5 títulos com mais exemplares vendidos."
              itens={dados.livrosMaisVendidos || []}
            />
            <Ranking
              titulo="Matérias mais vendidas"
              descricao="As 5 matérias com mais exemplares vendidos."
              itens={dados.materiasMaisVendidas || []}
            />
          </div>

          <h3 className="dashboard-titulo">Pedidos por status</h3>
          <div className="vendas-status">
            {Object.entries(dados.pedidosPorStatus || {}).map(([status, quantidade]) => (
              <div className="vendas-status-item" key={status}>
                <span>{ROTULO_STATUS[status] || status}</span>
                <strong>{numero(quantidade)}</strong>
                <small>
                  {totalPedidos > 0 ? Math.round((quantidade / totalPedidos) * 100) : 0}%
                </small>
              </div>
            ))}
          </div>
        </>
      )}
    </div>
  );
}
