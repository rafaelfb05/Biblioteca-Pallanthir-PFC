package br.com.pfc.biblioteca.model;

import br.com.pfc.biblioteca.dto.DadosLivro;
import br.com.pfc.biblioteca.enums.Materia;
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
    private String capaUrl;
    @Enumerated(EnumType.STRING)
    private Materia materia;
    private double preco;
    private double avaliacao;

    public Livro(){}

    public Livro(DadosLivro dados){
        this.titulo=dados.titulo();
        if (dados.autores() == null || dados.autores().isEmpty()) {
            this.autores = List.of("Autor desconhecido");
        } else {
            this.autores = dados.autores();
        }
        this.anoLancamento=dados.anoLancamento();
        this.numeroPagina=dados.numeroPagina();
        if (dados.imagem() == null) {
            System.out.println("Capa não encontrada");
            this.capaUrl = null;
        } else {
            this.capaUrl = dados.imagem().capaUrl();
        }
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

    public String getCapaUrl() {
        return capaUrl;
    }

    public void setCapaUrl(String capaUrl) {
        this.capaUrl = capaUrl;
    }

    public Materia getMateria() {
        return materia;
    }

    public void setMateria(Materia materia) {
        this.materia = materia;
    }

    public double getPreco() {
        return preco;
    }

    public void setPreco(double preco) {
        this.preco = preco;
    }

    @Override
    public String toString() {
        return "Livro = " + titulo + '\'' +
                ", autor = " + autores + '\'' +
                ", anoLancamento = " + anoLancamento +
                ", numeroPagina = " + numeroPagina +
                ", capa = " + capaUrl +
                ", matéria = " + materia +
                ", preço = " + preco +
                ", avaliacao = " + avaliacao;
    }
}