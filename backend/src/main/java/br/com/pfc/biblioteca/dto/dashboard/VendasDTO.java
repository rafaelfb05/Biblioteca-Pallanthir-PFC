package br.com.pfc.biblioteca.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record VendasDTO(LocalDate de,
                        LocalDate ate,
                        Resumo resumo,
                        Map<String, Long> pedidosPorStatus,
                        List<ItemRanking> livrosMaisVendidos,
                        List<ItemRanking> materiasMaisVendidas,
                        List<VendaMensal> vendasPorMes,
                        Multas multas) {

    public record Resumo(BigDecimal faturamento, long pedidosPagos, BigDecimal ticketMedio,
                         long exemplaresVendidos, long aguardandoRetirada, long entregues,
                         long reembolsados, BigDecimal valorReembolsado) {
    }

    public record ItemRanking(String nome, String rotulo, long quantidade, BigDecimal receita) {
    }

    public record VendaMensal(String mes, BigDecimal faturamento, long pedidos) {
    }

    public record Multas(BigDecimal valorRecebido, long recebidas, BigDecimal valorPendente, long pendentes) {
    }
}
