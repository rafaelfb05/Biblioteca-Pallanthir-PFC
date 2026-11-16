package br.com.pfc.biblioteca.dto.usuario;

import br.com.pfc.biblioteca.enums.TipoUsuario;
import jakarta.validation.constraints.NotNull;

public record AlterarPerfilRequest(@NotNull(message = "Informe o novo perfil") TipoUsuario tipo) {
}
