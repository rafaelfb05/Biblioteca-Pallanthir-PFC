package br.com.pfc.biblioteca.repository;

import br.com.pfc.biblioteca.entity.jpa.Pedido;
import br.com.pfc.biblioteca.enums.StatusPedido;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByUsuarioIdOrderByDataCriacaoDesc(Long usuarioId);

    List<Pedido> findByStatusAndDataExpiracaoBefore(StatusPedido status, LocalDateTime limite);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Pedido p WHERE p.id = :id")
    Optional<Pedido> BuscarComLock(Long id);
}
