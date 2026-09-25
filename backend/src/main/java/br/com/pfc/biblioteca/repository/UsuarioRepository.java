package br.com.pfc.biblioteca.repository;

import br.com.pfc.biblioteca.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    List<Usuario> findByAtivoTrue();

    Optional<Usuario> findByEmailAndAtivoTrue(String email);

    Optional<Usuario> findByEmailAndSenhaAndAtivoTrue(String email, String senha);

    @Query("SELECT u FROM Usuario u JOIN u.favoritos f WHERE f.id = :livroId")
    List<Usuario> buscarPorLivroFavoritado(Long livroId);
}
