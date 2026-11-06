package br.com.pfc.biblioteca.dto.dashboard;

import br.com.pfc.biblioteca.dto.livro.MateriaDTO;
import br.com.pfc.biblioteca.dto.reserva.ReservasResumoDTO;

import java.util.List;

public record DashboardDTO(EstoqueResumoDTO estoque,
                           ReservasResumoDTO reservas,
                           List<MateriaDTO> materiasMaisReservadas,
                           List<MateriaDTO> materiasMaisCompradas) {
}
