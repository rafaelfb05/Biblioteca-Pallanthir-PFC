package br.com.pfc.biblioteca.dto.auth;

import br.com.pfc.biblioteca.dto.usuario.UsuarioDTO;

public record LoginResponse(UsuarioDTO usuario, String token) {
}
