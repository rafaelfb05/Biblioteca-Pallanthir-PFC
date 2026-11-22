package br.com.pfc.biblioteca.dto.multa;

import br.com.pfc.biblioteca.entity.jpa.Multa;
import br.com.pfc.biblioteca.enums.StatusMulta;
import br.com.pfc.biblioteca.service.MascaraDeEmail;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record MultaDTO(UUID id, UUID livroId, String tituloLivro, String capaUrl, BigDecimal valor,
                       String motivo, StatusMulta status, LocalDateTime dataCriacao, LocalDateTime dataPagamento,
                       UUID usuarioId, String nomeEstudante, String emailEstudante, String linkPagamento) {

    public MultaDTO(Multa multa) {
        this(multa.getId(), multa.getLivro().getId(), multa.getLivro().getTitulo(), multa.getLivro().getCapaUrl(),
                multa.getValor(), multa.getMotivo(), multa.getStatus(), multa.getDataCriacao(), multa.getDataPagamento(),
                multa.getUsuario().getId(), multa.getUsuario().getNome(), MascaraDeEmail.mascarar(multa.getUsuario().getEmail()),
                multa.getStatus() == StatusMulta.PENDENTE ? multa.getLinkPagamento() : null);
    }
}
