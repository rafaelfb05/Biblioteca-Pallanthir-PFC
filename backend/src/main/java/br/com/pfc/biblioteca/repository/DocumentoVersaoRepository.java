package br.com.pfc.biblioteca.repository;

import br.com.pfc.biblioteca.entity.jpa.DocumentoVersao;
import br.com.pfc.biblioteca.enums.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentoVersaoRepository extends JpaRepository<DocumentoVersao, UUID> {

    List<DocumentoVersao> findByTipoOrderByNumeroDesc(TipoDocumento tipo);

    long countByTipo(TipoDocumento tipo);
}
