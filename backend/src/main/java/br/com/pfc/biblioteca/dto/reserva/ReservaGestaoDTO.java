package br.com.pfc.biblioteca.dto.reserva;

import br.com.pfc.biblioteca.entity.jpa.Reserva;
import br.com.pfc.biblioteca.enums.StatusReserva;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReservaGestaoDTO(UUID id, UUID livroId, String tituloLivro, String capaUrl,
                               LocalDateTime dataReserva, StatusReserva status,
                               UUID usuarioId, String nomeEstudante, String emailEstudante,
                               LocalDateTime prazoRetirada) {

    public ReservaGestaoDTO(Reserva reserva) {
        this(reserva.getId(), reserva.getLivro().getId(), reserva.getLivro().getTitulo(),
                reserva.getLivro().getCapaUrl(), reserva.getDataReserva(), reserva.getStatus(),
                reserva.getUsuario().getId(), reserva.getUsuario().getNome(), reserva.getUsuario().getEmail(),
                reserva.getPrazoRetirada());
    }
}
