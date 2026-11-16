package br.com.pfc.biblioteca.service;

import br.com.pfc.biblioteca.dto.dashboard.VendasDTO;
import br.com.pfc.biblioteca.entity.jpa.ItemPedido;
import br.com.pfc.biblioteca.entity.jpa.Multa;
import br.com.pfc.biblioteca.entity.jpa.Pedido;
import br.com.pfc.biblioteca.enums.StatusMulta;
import br.com.pfc.biblioteca.enums.StatusPedido;
import br.com.pfc.biblioteca.infra.exception.RegraDeNegocioException;
import br.com.pfc.biblioteca.repository.MultaRepository;
import br.com.pfc.biblioteca.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class VendasService {

    private static final Set<StatusPedido> PAGOS = EnumSet.of(StatusPedido.APROVADO, StatusPedido.ENTREGUE);
    private static final int TAMANHO_RANKING = 5;

    private final PedidoRepository pedidoRepository;
    private final MultaRepository multaRepository;

    public VendasService(PedidoRepository pedidoRepository, MultaRepository multaRepository) {
        this.pedidoRepository = pedidoRepository;
        this.multaRepository = multaRepository;
    }

    @Transactional(readOnly = true)
    public VendasDTO montar(LocalDate de, LocalDate ate) {
        if (de != null && ate != null && de.isAfter(ate)) {
            throw new RegraDeNegocioException("A data inicial não pode ser posterior à data final");
        }
        LocalDateTime inicio = de == null ? LocalDateTime.of(2000, 1, 1, 0, 0) : de.atStartOfDay();
        LocalDateTime fim = ate == null ? LocalDateTime.now().plusYears(1) : ate.plusDays(1).atStartOfDay().minusNanos(1);

        List<Pedido> pedidos = pedidoRepository.findByDataCriacaoBetween(inicio, fim);
        List<Pedido> pagos = pedidos.stream().filter(p -> PAGOS.contains(p.getStatus())).toList();
        List<Pedido> reembolsados = pedidos.stream().filter(p -> p.getStatus() == StatusPedido.REEMBOLSADO).toList();
        List<ItemPedido> itensPagos = pagos.stream().flatMap(p -> p.getItens().stream()).toList();

        BigDecimal faturamento = somar(pagos);
        BigDecimal ticketMedio = pagos.isEmpty()
                ? BigDecimal.ZERO
                : faturamento.divide(BigDecimal.valueOf(pagos.size()), 2, RoundingMode.HALF_UP);

        VendasDTO.Resumo resumo = new VendasDTO.Resumo(
                faturamento,
                pagos.size(),
                ticketMedio,
                itensPagos.stream().mapToLong(ItemPedido::getQuantidade).sum(),
                contar(pedidos, StatusPedido.APROVADO),
                contar(pedidos, StatusPedido.ENTREGUE),
                reembolsados.size(),
                somar(reembolsados));

        Map<String, Long> porStatus = new LinkedHashMap<>();
        for (StatusPedido status : StatusPedido.values()) {
            porStatus.put(status.name(), contar(pedidos, status));
        }

        return new VendasDTO(de, ate, resumo, porStatus,
                ranking(itensPagos, item -> item.getLivro().getId().toString(), item -> item.getLivro().getTitulo()),
                ranking(itensPagos.stream().filter(item -> item.getLivro().getMateria() != null).toList(),
                        item -> item.getLivro().getMateria().name(),
                        item -> item.getLivro().getMateria().getMateriaLivro()),
                porMes(pagos),
                multas(multaRepository.findByDataCriacaoBetween(inicio, fim)));
    }

    private long contar(List<Pedido> pedidos, StatusPedido status) {
        return pedidos.stream().filter(p -> p.getStatus() == status).count();
    }

    private BigDecimal somar(List<Pedido> pedidos) {
        return pedidos.stream()
                .map(Pedido::getTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal receita(ItemPedido item) {
        return item.getPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade()));
    }

    private List<VendasDTO.ItemRanking> ranking(List<ItemPedido> itens, Function<ItemPedido, String> chave,
                                                Function<ItemPedido, String> rotulo) {
        Map<String, List<ItemPedido>> grupos = itens.stream()
                .collect(Collectors.groupingBy(chave, LinkedHashMap::new, Collectors.toList()));
        return grupos.entrySet().stream()
                .map(grupo -> new VendasDTO.ItemRanking(
                        grupo.getKey(),
                        rotulo.apply(grupo.getValue().get(0)),
                        grupo.getValue().stream().mapToLong(ItemPedido::getQuantidade).sum(),
                        grupo.getValue().stream().map(this::receita).reduce(BigDecimal.ZERO, BigDecimal::add)))
                .sorted(Comparator.comparingLong(VendasDTO.ItemRanking::quantidade).reversed()
                        .thenComparing(VendasDTO.ItemRanking::receita, Comparator.reverseOrder()))
                .limit(TAMANHO_RANKING)
                .toList();
    }

    private List<VendasDTO.VendaMensal> porMes(List<Pedido> pagos) {
        Map<YearMonth, List<Pedido>> meses = pagos.stream()
                .collect(Collectors.groupingBy(p -> YearMonth.from(p.getDataCriacao()), TreeMap::new, Collectors.toList()));
        return meses.entrySet().stream()
                .map(mes -> new VendasDTO.VendaMensal(mes.getKey().toString(), somar(mes.getValue()), mes.getValue().size()))
                .toList();
    }

    private VendasDTO.Multas multas(List<Multa> multas) {
        List<Multa> pagas = multas.stream().filter(m -> m.getStatus() == StatusMulta.PAGA).toList();
        List<Multa> pendentes = multas.stream().filter(m -> m.getStatus() == StatusMulta.PENDENTE).toList();
        return new VendasDTO.Multas(somarMultas(pagas), pagas.size(), somarMultas(pendentes), pendentes.size());
    }

    private BigDecimal somarMultas(List<Multa> multas) {
        return multas.stream().map(Multa::getValor).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
