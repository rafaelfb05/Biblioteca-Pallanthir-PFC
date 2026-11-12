package br.com.pfc.biblioteca.dto.carrinho;

import java.util.UUID;
import java.math.BigDecimal;

public record ItemCarrinhoDTO(UUID livroId, String titulo, String capaUrl,
                              BigDecimal precoUnitario, Integer quantidade, BigDecimal subtotal) {
}
