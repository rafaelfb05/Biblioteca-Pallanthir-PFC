package br.com.pfc.biblioteca.model;

import br.com.pfc.biblioteca.dto.DadosLivro;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "livros")
public class Livro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String titulo;
    private List<String> autores;
    private String anoLancamento;
    private int numeroPagina;
    private double avaliacao;

    public Livro(){}

    public Livro(DadosLivro dadosLivro){
        this.titulo=dadosLivro.titulo();
        this.autores=dadosLivro.autores();
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

    public List<String> getAutores() {
        return autores;
    }

    public void setAutores(List<String> autores) {
        this.autores = autores;
    }

    public String getAnoLancamento() {
        return anoLancamento;
    }

    public void setAnoLancamento(String anoLancamento) {
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
                ", autor = " + autores + '\'' +
                ", anoLancamento = " + anoLancamento +
                ", numeroPagina = " + numeroPagina +
                ", avaliacao = " + avaliacao;
    }
}
