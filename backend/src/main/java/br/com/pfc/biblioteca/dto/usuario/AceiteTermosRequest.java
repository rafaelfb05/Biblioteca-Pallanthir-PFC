package br.com.pfc.biblioteca.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AceiteTermosRequest(@NotBlank @Size(max = 64) String versao) {
}
