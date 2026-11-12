package br.com.pfc.biblioteca.dto.reserva;

import java.util.UUID;
import br.com.pfc.biblioteca.enums.StatusReserva;
import br.com.pfc.biblioteca.entity.jpa.Reserva;

import java.time.LocalDateTime;

public record ReservaDTO(UUID id, UUID livroId, String tituloLivro, LocalDateTime dataReserva, StatusReserva status) {

    public ReservaDTO(Reserva reserva){
        this(reserva.getId(), reserva.getLivro().getId(), reserva.getLivro().getTitulo(),
                reserva.getDataReserva(), reserva.getStatus());
    }
}
