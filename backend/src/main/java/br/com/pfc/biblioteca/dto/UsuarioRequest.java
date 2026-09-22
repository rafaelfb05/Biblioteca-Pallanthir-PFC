package br.com.pfc.biblioteca.dto;

import br.com.pfc.biblioteca.enums.TipoUsuario;

public record UsuarioRequest(String nome, String email, String senha, TipoUsuario tipo, String codigoAcesso) {
}
