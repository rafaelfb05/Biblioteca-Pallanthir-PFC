package br.com.pfc.biblioteca.repository;

import br.com.pfc.biblioteca.entity.jpa.AvisoDisponibilidade;
import br.com.pfc.biblioteca.enums.StatusAviso;
import br.com.pfc.biblioteca.enums.TipoEstoque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AvisoDisponibilidadeRepository extends JpaRepository<AvisoDisponibilidade, UUID> {

    List<AvisoDisponibilidade> findByUsuarioIdAndStatusOrderByDataCriacaoDesc(UUID usuarioId, StatusAviso status);

    Optional<AvisoDisponibilidade> findFirstByUsuarioIdAndLivroIdAndTipoAndStatus(UUID usuarioId, UUID livroId,
                                                                                  TipoEstoque tipo, StatusAviso status);

    @Query("""
        SELECT a.id FROM AvisoDisponibilidade a
        WHERE a.status = :status AND a.livro.ativo = true
          AND ((a.tipo = :reserva AND a.livro.estoqueReserva > 0)
            OR (a.tipo = :compra AND a.livro.estoqueCompra > 0 AND a.livro.preco > 0))
        ORDER BY a.dataCriacao
        """)
    List<UUID> prontosParaEnvio(StatusAviso status, TipoEstoque reserva, TipoEstoque compra);
}
