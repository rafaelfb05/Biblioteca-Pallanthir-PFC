package br.com.pfc.biblioteca.repository;

import br.com.pfc.biblioteca.entity.jpa.Pedido;
import br.com.pfc.biblioteca.enums.StatusPedido;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.awt.print.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByUsuarioIdOrderByDataCriacaoDesc(Long usuarioId);

    List<Pedido> findByUsuarioIdAndStatus(Long usuarioId, StatusPedido status);

    boolean existsByUsuarioIdAndStatus(Long usuarioId, StatusPedido status);

    List<Pedido> findByStatusAndDataExpiracaoBefore(StatusPedido status, LocalDateTime limite);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Pedido p WHERE p.id = :id")
    Optional<Pedido> buscarComLock(Long id);

    @Query("""
        SELECT i.livro.materia, SUM(i.quantidade) FROM ItemPedido i
        WHERE i.pedido.status = :status AND i.livro.materia IS NOT NULL
        GROUP BY i.livro.materia
        ORDER BY SUM(i.quantidade) DESC
        """)
    List<Object[]> materiasMaisCompradas(StatusPedido status, PageRequest pageRequest);
}
