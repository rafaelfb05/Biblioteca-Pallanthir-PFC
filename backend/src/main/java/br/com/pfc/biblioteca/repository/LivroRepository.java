package br.com.pfc.biblioteca.repository;

import java.util.UUID;

import br.com.pfc.biblioteca.enums.Materia;
import br.com.pfc.biblioteca.entity.jpa.Livro;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LivroRepository extends JpaRepository<Livro, UUID> {

    List<Livro> findByAtivoTrue();

    long countByAtivoTrue();

    List<Livro> findByMateriaAndAtivoTrue(Materia materia);

    long countByMateria(Materia materia);

    Optional<Livro> findByTituloContainingIgnoreCaseAndAtivoTrue(String titulo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Livro l WHERE l.id = :id")
    Optional<Livro> buscarComLock(UUID id);

@Query("SELECT COALESCE(SUM(l.estoqueReserva), 0L) FROM Livro l WHERE l.ativo = true")
    long somarEstoqueReserva();

@Query("SELECT COALESCE(SUM(l.estoqueCompra), 0L) FROM Livro l WHERE l.ativo = true")
    long somarEstoqueCompra();

@Query("SELECT COUNT(l) FROM Livro l WHERE l.ativo = true "
        + "AND (l.estoqueReserva IS NULL OR l.estoqueReserva <= 0)")
    long contarEsgotadosParaReserva();
}
