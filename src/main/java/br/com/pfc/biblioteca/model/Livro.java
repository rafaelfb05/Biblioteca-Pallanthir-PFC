package br.com.pfc.biblioteca.model;

import br.com.pfc.biblioteca.dto.DadosLivro;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.persistence.*;

@Entity
@Table(name = "livros")
public class Livro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String titulo;
    private String autor;
    private int anoLancamento;
    private int numeroPagina;
    private double avaliacao;

    public Livro(){}

    public Livro(DadosLivro dadosLivro){
        this.titulo=dadosLivro.titulo();
        this.autor=dadosLivro.autor();
        this.anoLancamento=dadosLivro.anoLancamento();
        this.numeroPagina=dadosLivro.numeroPagina();
        this.avaliacao=dadosLivro.avalliacao();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public int getAnoLancamento() {
        return anoLancamento;
    }

    public void setAnoLancamento(int anoLancamento) {
        this.anoLancamento = anoLancamento;
    }

    public int getNumeroPagina() {
        return numeroPagina;
    }

    public void setNumeroPagina(int numeroPagina) {
        this.numeroPagina = numeroPagina;
    }

    public double getAvaliacao() {
        return avaliacao;
    }

    public void setAvaliacao(double avaliacao) {
        this.avaliacao = avaliacao;
    }

    @Override
    public String toString() {
        return "Livro = " + titulo + '\'' +
                ", autor = " + autor + '\'' +
                ", anoLancamento = " + anoLancamento +
                ", numeroPagina = " + numeroPagina +
                ", avaliacao = " + avaliacao;
    }
}
