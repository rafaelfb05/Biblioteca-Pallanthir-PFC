package br.com.pfc.biblioteca.dto.documento;

import br.com.pfc.biblioteca.entity.jpa.DocumentoVersao;
import br.com.pfc.biblioteca.enums.TipoDocumento;

import java.time.LocalDateTime;
import java.util.UUID;

public record DocumentoVersaoDTO(UUID id, TipoDocumento tipo, int numero, String conteudo,
                                 LocalDateTime publicadoEm, String publicadoPor, LocalDateTime substituidaEm) {

    public static DocumentoVersaoDTO resumo(DocumentoVersao versao) {
        return new DocumentoVersaoDTO(versao.getId(), versao.getTipo(), versao.getNumero(), null,
                versao.getPublicadoEm(), versao.getPublicadoPorNome(), versao.getArquivadoEm());
    }

    public static DocumentoVersaoDTO completo(DocumentoVersao versao) {
        return new DocumentoVersaoDTO(versao.getId(), versao.getTipo(), versao.getNumero(), versao.getConteudo(),
                versao.getPublicadoEm(), versao.getPublicadoPorNome(), versao.getArquivadoEm());
    }
}
