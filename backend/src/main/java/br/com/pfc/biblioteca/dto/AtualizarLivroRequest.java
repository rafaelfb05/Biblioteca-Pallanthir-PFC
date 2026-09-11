package br.com.pfc.biblioteca.dto;

import br.com.pfc.biblioteca.enums.Materia;

import java.util.List;

public record AtualizarLivroRequest(String titulo,
                                    List<String> autores,
                                    String anoLancamento,
                                    Integer numeroPagina,
                                    Materia materia,
                                    Double preco,
                                    Double avaliacao) {
}
