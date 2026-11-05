package br.com.pfc.biblioteca.dto.auth;

public record RedefinirSenhaRequest(String email, String codigo, String novaSenha) {
}
