package br.com.pfc.biblioteca.repository;

import java.util.UUID;
import br.com.pfc.biblioteca.enums.StatusReserva;
import br.com.pfc.biblioteca.entity.jpa.Reserva;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.awt.print.Pageable;
import java.util.List;
import java.util.Objects;

public interface ReservaRepository extends JpaRepository<Reserva, UUID> {
    List<Reserva> findByUsuarioId(UUID usuarioId);

    List<Reserva> findByLivroId(UUID livroId);

    boolean existsByUsuarioIdAndStatusIn(UUID usuarioId, List<StatusReserva> status);

    boolean existsByLivroIdAndStatusIn(UUID livroId, List<StatusReserva> status);

    long countByStatus(StatusReserva status);

    @Query("""
        SELECT r.livro.materia, COUNT(r) FROM Reserva r
        WHERE r.livro.materia IS NOT NULL AND r.status <> :excluido
        GROUP BY r.livro.materia
        ORDER BY COUNT(r) DESC
        """)
    List<Object[]> materiasMaisReservadas(StatusReserva excluido, PageRequest pageRequest);
}
