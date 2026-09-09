package br.com.pfc.biblioteca.repository;


import br.com.pfc.biblioteca.model.Livro;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LivroRepository extends JpaRepository<Livro, Long> {


}
