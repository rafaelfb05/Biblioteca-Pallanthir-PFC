package br.com.pfc.biblioteca.repository;

import br.com.pfc.biblioteca.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmailAndSenha(String email, String senha);

    @Query("SELECT u FROM Usuario u JOIN u.favoritos f WHERE f.id = :livroId")
    List<Usuario> buscarPorLivroFavoritado(Long livroId);
}
