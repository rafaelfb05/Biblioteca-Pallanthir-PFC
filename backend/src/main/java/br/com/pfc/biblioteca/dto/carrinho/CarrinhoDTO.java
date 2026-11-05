package br.com.pfc.biblioteca.dto.carrinho;

import java.math.BigDecimal;
import java.util.List;

public record CarrinhoDTO(List<ItemCarrinhoDTO> itens, BigDecimal total) {
}
