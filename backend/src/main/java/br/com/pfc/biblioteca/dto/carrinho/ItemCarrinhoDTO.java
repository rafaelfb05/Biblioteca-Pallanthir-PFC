package br.com.pfc.biblioteca.dto.carrinho;

import java.math.BigDecimal;

public record ItemCarrinhoDTO(Long livroId, String titulo, String capaUrl,
                              BigDecimal precoUnitario, Integer quantidade, BigDecimal subtotal) {
}
