package br.com.pfc.biblioteca.dto.pedido;

import br.com.pfc.biblioteca.entity.jpa.Pedido;
import br.com.pfc.biblioteca.service.MascaraDeEmail;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public record PedidoRetiradaDTO(UUID id, String nomeEstudante, BigDecimal total,
                                LocalDateTime dataCriacao, List<ItemPedidoDTO> itens,
                                UUID usuarioId, String emailEstudante, LocalDateTime prazoRetirada) {
    public PedidoRetiradaDTO(Pedido p) {
        this(p.getId(), p.getUsuario().getNome(), p.getTotal(), p.getDataCriacao(),
                p.getItens().stream().map(ItemPedidoDTO::new).collect(Collectors.toList()),
                p.getUsuario().getId(), MascaraDeEmail.mascarar(p.getUsuario().getEmail()), p.getPrazoRetirada());
    }
}
