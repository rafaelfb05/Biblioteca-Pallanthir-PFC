package br.com.pfc.biblioteca.dto.aviso;

import br.com.pfc.biblioteca.enums.TipoEstoque;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record NovoAvisoRequest(@NotNull(message = "Informe o livro") UUID livroId,
                               @NotNull(message = "Informe o estoque") TipoEstoque tipo) {
}
