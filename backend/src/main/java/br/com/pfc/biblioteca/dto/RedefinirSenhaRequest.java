package br.com.pfc.biblioteca.dto;

public record RedefinirSenhaRequest(String email, String codigo, String novaSenha) {
}
