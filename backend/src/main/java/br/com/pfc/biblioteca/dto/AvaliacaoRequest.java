package br.com.pfc.biblioteca.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AvaliacaoRequest(
        @NotNull @Min(1) @Max(5) Integer nota,
        @Size(max = 500) String comentario
) {
}
