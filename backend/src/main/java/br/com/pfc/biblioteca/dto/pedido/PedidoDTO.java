package br.com.pfc.biblioteca.dto.pedido;

import java.util.UUID;
import br.com.pfc.biblioteca.entity.jpa.Pedido;
import br.com.pfc.biblioteca.enums.StatusPedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public record PedidoDTO(UUID id, StatusPedido status, BigDecimal total,
                        LocalDateTime dataCriacao, LocalDateTime dataExpiracao,
                        String linkPagamento, List<ItemPedidoDTO> itens) {

    public PedidoDTO(Pedido p){
        this(p.getId(), p.getStatus(), p.getTotal(), p.getDataCriacao(), p.getDataExpiracao(),
                p.getLinkPagamento(), p.getItens().stream().map(ItemPedidoDTO::new).collect(Collectors.toList()));
    }
}
