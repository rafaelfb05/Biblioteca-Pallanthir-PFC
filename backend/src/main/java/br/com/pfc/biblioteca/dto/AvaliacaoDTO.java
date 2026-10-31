package br.com.pfc.biblioteca.dto;

import br.com.pfc.biblioteca.entity.jpa.Avaliacao;

import java.time.LocalDateTime;

public record AvaliacaoDTO(Long id, String nomeUsuario, Integer nota,
                           String comentario, LocalDateTime dataAvaliacao) {

    public AvaliacaoDTO(Avaliacao a){
        this(a.getId(), a.getUsuario().getNome(), a.getNota(),
                a.getComentario(), a.getDataAvaliacao());
    }
}
