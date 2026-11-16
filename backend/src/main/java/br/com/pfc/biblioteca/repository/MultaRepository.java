package br.com.pfc.biblioteca.repository;

import br.com.pfc.biblioteca.entity.jpa.Multa;
import br.com.pfc.biblioteca.enums.StatusMulta;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MultaRepository extends JpaRepository<Multa, UUID> {

    List<Multa> findByUsuarioIdOrderByDataCriacaoDesc(UUID usuarioId);

    List<Multa> findByUsuarioIdAndStatus(UUID usuarioId, StatusMulta status);

    boolean existsByUsuarioIdAndStatus(UUID usuarioId, StatusMulta status);

    List<Multa> findByStatusOrderByDataCriacaoAsc(StatusMulta status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Multa m WHERE m.id = :id")
    Optional<Multa> buscarComLock(UUID id);
}
