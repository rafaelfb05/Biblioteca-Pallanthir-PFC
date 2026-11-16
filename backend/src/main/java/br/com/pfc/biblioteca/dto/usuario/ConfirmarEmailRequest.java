package br.com.pfc.biblioteca.dto.usuario;

import jakarta.validation.constraints.NotBlank;

public record ConfirmarEmailRequest(@NotBlank(message = "Informe o código") String codigo) {
}
