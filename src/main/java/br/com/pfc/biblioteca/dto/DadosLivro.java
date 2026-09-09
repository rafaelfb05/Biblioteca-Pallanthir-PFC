package br.com.pfc.biblioteca.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DadosLivro(
        @JsonAlias("title") String titulo,
        @JsonAlias("authors") List<String> autores,
        @JsonAlias("publishedDate") String anoLancamento,
        @JsonAlias("pageCount") int numeroPagina,
        @JsonAlias("ImageLinks") ImagemLivro imagem,
        double avalliacao
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ImagemLivro(@JsonAlias("thumbnail") String capaUrl) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DadosBusca(@JsonAlias("items") List<DadosItem> items) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DadosItem(@JsonAlias("volumeInfo") DadosLivro volumeInfo) {}
}
