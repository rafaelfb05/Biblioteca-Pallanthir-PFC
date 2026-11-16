package br.com.pfc.biblioteca.dto.documento;

import br.com.pfc.biblioteca.entity.jpa.Documento;
import br.com.pfc.biblioteca.enums.TipoDocumento;

import java.time.LocalDateTime;

public record DocumentoDTO(TipoDocumento tipo, String titulo, String conteudo,
                           LocalDateTime atualizadoEm, String atualizadoPor) {

    public DocumentoDTO(Documento documento) {
        this(documento.getTipo(), documento.getTipo().getTitulo(), documento.getConteudo(),
                documento.getAtualizadoEm(), documento.getAtualizadoPorNome());
    }
}
