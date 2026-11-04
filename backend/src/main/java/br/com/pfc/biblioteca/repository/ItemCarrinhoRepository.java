package br.com.pfc.biblioteca.repository;

import br.com.pfc.biblioteca.entity.jpa.ItemCarrinho;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ItemCarrinhoRepository extends JpaRepository<ItemCarrinho, Long> {

    Optional<ItemCarrinho> findByUsuarioIdAndLivroId(Long usuarioId, Long livroId);
    List<ItemCarrinho> findByUsuarioIdAndAtivoTrue(Long usuarioId);
}
