package br.com.pfc.biblioteca.dto.carrinho;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AdicionarCarrinhoRequest(@NotNull Long livroId,
                                       @NotNull @Min(1) Integer quantidade) {
}
