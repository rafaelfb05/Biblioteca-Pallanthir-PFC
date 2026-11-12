package br.com.pfc.biblioteca.repository;

import java.util.UUID;
import br.com.pfc.biblioteca.entity.jpa.ItemCarrinho;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ItemCarrinhoRepository extends JpaRepository<ItemCarrinho, UUID> {

    Optional<ItemCarrinho> findByUsuarioIdAndLivroId(UUID usuarioId, UUID livroId);
    List<ItemCarrinho> findByUsuarioIdAndAtivoTrue(UUID usuarioId);
}
