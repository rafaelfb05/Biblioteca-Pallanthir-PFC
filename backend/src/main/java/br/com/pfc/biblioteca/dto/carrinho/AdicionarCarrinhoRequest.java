package br.com.pfc.biblioteca.dto.carrinho;

import java.util.UUID;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AdicionarCarrinhoRequest(@NotNull UUID livroId,
                                       @NotNull @Min(1) Integer quantidade) {
}
