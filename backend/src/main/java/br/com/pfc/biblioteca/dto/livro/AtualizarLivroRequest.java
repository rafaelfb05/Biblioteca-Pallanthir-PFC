package br.com.pfc.biblioteca.dto.livro;

import br.com.pfc.biblioteca.enums.Materia;

import java.util.List;

public record AtualizarLivroRequest(String titulo,
                                    List<String> autores,
                                    String anoLancamento,
                                    Integer numeroPagina,
                                    Materia materia,
                                    Double preco,
                                    Integer estoqueReserva,
                                    Integer estoqueCompra,
                                    String isbn10,
                                    String isbn13) {
}
