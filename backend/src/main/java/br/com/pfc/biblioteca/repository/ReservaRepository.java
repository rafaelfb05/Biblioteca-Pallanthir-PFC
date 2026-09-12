package br.com.pfc.biblioteca.repository;

import br.com.pfc.biblioteca.enums.StatusReserva;
import br.com.pfc.biblioteca.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    List<Reserva> findByUsuarioId(Long usuarioId);

    List<Reserva> findByLivroId(Long livroId);

    boolean existsByLivroIdAndStatus(Long livroId, StatusReserva status);
}
