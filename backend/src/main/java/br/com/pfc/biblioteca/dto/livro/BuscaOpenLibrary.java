package br.com.pfc.biblioteca.dto.livro;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BuscaOpenLibrary(@JsonAlias("docs") List<Resultado> docs) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Resultado(@JsonAlias("title") String titulo,
                            @JsonAlias("author_name") List<String> autores,
                            @JsonAlias("cover_i") Long capaId) {
    }
}
