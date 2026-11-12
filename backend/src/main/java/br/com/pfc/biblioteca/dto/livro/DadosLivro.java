package br.com.pfc.biblioteca.dto.livro;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DadosLivro(
        @JsonAlias("title") String titulo,
        @JsonAlias("authors") List<String> autores,
        @JsonAlias("publishedDate") String anoLancamento,
        @JsonAlias("pageCount") int numeroPagina,
        @JsonAlias("imageLinks") ImagemLivro imagem,
        @JsonAlias("industryIdentifiers") List<Identificador> identificadores
) {
    public String isbn(String tipo) {
        if (identificadores == null) {
            return null;
        }
        return identificadores.stream()
                .filter(identificador -> tipo.equals(identificador.tipo()))
                .map(Identificador::codigo)
                .findFirst()
                .orElse(null);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ImagemLivro(@JsonAlias("thumbnail") String capaUrl) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Identificador(@JsonAlias("type") String tipo, @JsonAlias("identifier") String codigo) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DadosBusca(@JsonAlias("items") List<DadosItem> items) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DadosItem(@JsonAlias("volumeInfo") DadosLivro volumeInfo) {}
}
