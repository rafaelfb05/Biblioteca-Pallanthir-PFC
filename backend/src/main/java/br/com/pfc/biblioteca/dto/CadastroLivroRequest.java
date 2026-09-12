package br.com.pfc.biblioteca.dto;

import br.com.pfc.biblioteca.enums.Materia;

public record CadastroLivroRequest(String titulo, Materia materia, Double preco, Integer estoque) {
}
