package br.com.pfc.biblioteca.dto;

import br.com.pfc.biblioteca.enums.StatusReserva;
import br.com.pfc.biblioteca.model.Reserva;

import java.time.LocalDateTime;

public record ReservaDTO(Long id, Long livroId, String tituloLivro, LocalDateTime dataReserva, StatusReserva status) {

    public ReservaDTO(Reserva reserva){
        this(reserva.getId(), reserva.getLivro().getId(), reserva.getLivro().getTitulo(),
                reserva.getDataReserva(), reserva.getStatus());
    }
}
