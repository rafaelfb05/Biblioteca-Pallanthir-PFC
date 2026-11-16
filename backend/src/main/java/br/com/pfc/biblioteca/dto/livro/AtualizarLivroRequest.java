package br.com.pfc.biblioteca.dto.livro;

import jakarta.validation.constraints.PositiveOrZero;
import br.com.pfc.biblioteca.enums.Materia;

import java.util.List;

public record AtualizarLivroRequest(String titulo,
                                    List<String> autores,
                                    String anoLancamento,
                                    @PositiveOrZero(message = "O valor não pode ser negativo") Integer numeroPagina,
                                    Materia materia,
                                    @PositiveOrZero(message = "O valor não pode ser negativo") Double preco,
                                    @PositiveOrZero(message = "O valor não pode ser negativo") Integer estoqueReserva,
                                    @PositiveOrZero(message = "O valor não pode ser negativo") Integer estoqueCompra,
                                    String isbn10,
                                    String isbn13,
                                    String capaUrl) {
}
