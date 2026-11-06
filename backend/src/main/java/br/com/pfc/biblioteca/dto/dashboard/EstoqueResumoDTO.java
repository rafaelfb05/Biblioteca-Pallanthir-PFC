package br.com.pfc.biblioteca.dto.dashboard;

public record EstoqueResumoDTO(long titulosAtivos, long exemplaresParaReserva,
                               long exemplaresParaCompra, long esgotadosParaReserva) {
}
