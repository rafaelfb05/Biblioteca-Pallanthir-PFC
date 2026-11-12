package br.com.pfc.biblioteca.dto.pedido;

import java.util.UUID;
import br.com.pfc.biblioteca.entity.jpa.ItemPedido;

import java.math.BigDecimal;

public record ItemPedidoDTO(UUID livroId, String titulo, Integer quantidade, BigDecimal precoUnitario) {

    public ItemPedidoDTO(ItemPedido i){
        this(i.getLivro().getId(), i.getLivro().getTitulo(), i.getQuantidade(), i.getPrecoUnitario());
    }
}
