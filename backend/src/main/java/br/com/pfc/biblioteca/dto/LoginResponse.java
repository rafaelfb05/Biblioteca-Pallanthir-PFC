package br.com.pfc.biblioteca.dto;

public record LoginResponse(UsuarioDTO usuario, String token) {
}
