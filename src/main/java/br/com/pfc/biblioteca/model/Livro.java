package br.com.pfc.biblioteca.model;

import com.fasterxml.jackson.annotation.JsonAlias;

public class Livro {
    private String titulo;
    private String autor;
    private int anoLancamento;
    private int numeroPagina;
    private double avalliacao;
}
