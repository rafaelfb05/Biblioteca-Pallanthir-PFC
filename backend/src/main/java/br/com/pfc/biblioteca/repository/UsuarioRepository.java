package br.com.pfc.biblioteca.repository;

import br.com.pfc.biblioteca.enums.TipoUsuario;
import java.util.UUID;
import br.com.pfc.biblioteca.entity.jpa.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    List<Usuario> findByAtivoTrue();

    Optional<Usuario> findByEmailAndAtivoTrue(String email);

    Optional<Usuario> findByEmailAndSenhaAndAtivoTrue(String email, String senha);

    @Query("SELECT u FROM Usuario u JOIN u.favoritos f WHERE f.id = :livroId")
    List<Usuario> buscarPorLivroFavoritado(UUID livroId);

    long countByTipoAndAtivoTrue(TipoUsuario tipo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM Usuario u WHERE u.tipo = :tipo AND u.ativo = true")
    List<Usuario> travarAtivosPorTipo(TipoUsuario tipo);
}
