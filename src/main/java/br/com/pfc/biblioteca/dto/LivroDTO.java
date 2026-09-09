package br.com.pfc.biblioteca.dto;

import br.com.pfc.biblioteca.model.Livro;
import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

public record LivroDTO(String titulo,
                       List<String> autores,
                       String anoLancamento,
                       int numeroPagina,
                       String capaUrl,
                       double avalliacao) {

    public LivroDTO(Livro livro) {
        this(livro.getTitulo(),
        livro.getAutores(),
        livro.getAnoLancamento(),
        livro.getNumeroPagina(),
        livro.getCapaUrl(),
        livro.getAvaliacao());
    }
}
