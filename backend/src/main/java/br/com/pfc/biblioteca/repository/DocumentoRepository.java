package br.com.pfc.biblioteca.repository;

import br.com.pfc.biblioteca.entity.jpa.Documento;
import br.com.pfc.biblioteca.enums.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentoRepository extends JpaRepository<Documento, TipoDocumento> {
}
