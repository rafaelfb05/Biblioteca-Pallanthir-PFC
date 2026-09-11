package br.com.pfc.biblioteca.repository;


import br.com.pfc.biblioteca.enums.Materia;
import br.com.pfc.biblioteca.model.Livro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LivroRepository extends JpaRepository<Livro, Long> {


    List<Livro> findByMateria(Materia materia);

    long countByMateria(Materia materia);

    Optional<Livro> findByTituloContainingIgnoreCase(String titulo);
}
