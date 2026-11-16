package br.com.pfc.biblioteca.dto.reserva;

import java.math.BigDecimal;

public record DevolucaoRequest(Boolean danificado, BigDecimal valorMulta, String observacao) {
}
