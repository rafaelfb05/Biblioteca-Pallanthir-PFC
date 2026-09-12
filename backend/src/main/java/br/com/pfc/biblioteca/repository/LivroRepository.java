package br.com.pfc.biblioteca.repository;


import br.com.pfc.biblioteca.enums.Materia;
import br.com.pfc.biblioteca.model.Livro;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LivroRepository extends JpaRepository<Livro, Long> {


    List<Livro> findByMateria(Materia materia);

    long countByMateria(Materia materia);

    Optional<Livro> findByTituloContainingIgnoreCase(String titulo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Livro l WHERE l.id = :id")
    Optional<Livro> buscarComLock(Long id);
}
