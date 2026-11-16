package br.com.pfc.biblioteca.dto.aviso;

import br.com.pfc.biblioteca.entity.jpa.AvisoDisponibilidade;
import br.com.pfc.biblioteca.enums.StatusAviso;
import br.com.pfc.biblioteca.enums.TipoEstoque;

import java.time.LocalDateTime;
import java.util.UUID;

public record AvisoDisponibilidadeDTO(UUID id, UUID livroId, String tituloLivro, String capaUrl,
                                      TipoEstoque tipo, StatusAviso status, LocalDateTime dataCriacao) {

    public AvisoDisponibilidadeDTO(AvisoDisponibilidade aviso) {
        this(aviso.getId(), aviso.getLivro().getId(), aviso.getLivro().getTitulo(), aviso.getLivro().getCapaUrl(),
                aviso.getTipo(), aviso.getStatus(), aviso.getDataCriacao());
    }
}
