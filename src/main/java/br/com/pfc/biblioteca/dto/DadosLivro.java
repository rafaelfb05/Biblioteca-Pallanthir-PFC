package br.com.pfc.biblioteca.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DadosLivro(
        @JsonAlias("title") String titulo,
        @JsonAlias("author") String autor,
        @JsonAlias("publishedDate") int anoLancamento,
        @JsonAlias("pageCount") int numeroPagina,
        double avalliacao
) {
}
