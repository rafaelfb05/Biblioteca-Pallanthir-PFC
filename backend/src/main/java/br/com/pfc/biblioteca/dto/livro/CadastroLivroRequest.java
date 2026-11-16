package br.com.pfc.biblioteca.dto.livro;

import jakarta.validation.constraints.PositiveOrZero;
import br.com.pfc.biblioteca.enums.Materia;

public record CadastroLivroRequest(String titulo, Materia materia, @PositiveOrZero(message = "O valor não pode ser negativo") Double preco, @PositiveOrZero(message = "O valor não pode ser negativo") Integer estoqueReserva, @PositiveOrZero(message = "O valor não pode ser negativo") Integer estoqueCompra) {
}
