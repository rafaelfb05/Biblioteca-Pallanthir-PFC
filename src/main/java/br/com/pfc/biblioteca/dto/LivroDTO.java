package br.com.pfc.biblioteca.dto;

import br.com.pfc.biblioteca.enums.Materia;
import br.com.pfc.biblioteca.model.Livro;

import java.util.List;

public record LivroDTO(Long id,
                       String titulo,
                       List<String> autores,
                       String anoLancamento,
                       int numeroPagina,
                       String capaUrl,
                       Materia materia,
                       double avalliacao) {

    public LivroDTO(Livro livro) {
        this(livro.getId(), livro.getTitulo(),
        livro.getAutores(), livro.getAnoLancamento(),
        livro.getNumeroPagina(), livro.getCapaUrl(),
        livro.getMateria(), livro.getAvaliacao());
    }
}
