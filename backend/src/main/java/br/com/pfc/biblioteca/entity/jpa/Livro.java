package br.com.pfc.biblioteca.entity.jpa;

import java.util.UUID;
import br.com.pfc.biblioteca.dto.livro.DadosLivro;
import br.com.pfc.biblioteca.enums.Materia;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

import java.util.List;

@Entity
@Table(name = "livros")
public class Livro {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String titulo;
    private List<String> autores;
    private String anoLancamento;
    private int numeroPagina;
    private String capaUrl;
    @Enumerated(EnumType.STRING)
    private Materia materia;
    private Integer estoqueReserva = 1;
    private Integer estoqueCompra = 0;
    private Double preco = 0.0;
    @Column(length = 10)
    private String isbn10;
    @Column(length = 13)
    private String isbn13;
    @Column(nullable = false)
    @ColumnDefault("true")
    private boolean ativo = true;

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
        this.isbn10 = dados.isbn("ISBN_10");
        this.isbn13 = dados.isbn("ISBN_13");
    }

    public String getIsbn10() {
        return isbn10;
    }

    public void setIsbn10(String isbn10) {
        this.isbn10 = isbn10;
    }

    public String getIsbn13() {
        return isbn13;
    }

    public void setIsbn13(String isbn13) {
        this.isbn13 = isbn13;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
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

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public double getPreco() {
        if (preco == null){
            return 0.0;
        } else {
            return preco;
        }
    }

    public void setPreco(Double preco) {
        this.preco=preco;
    }

    public Integer getEstoqueReserva() {
        return estoqueReserva;
    }

    public void setEstoqueReserva(Integer estoqueReserva) {
        this.estoqueReserva = estoqueReserva;
    }

    public Integer getEstoqueCompra() {
        return estoqueCompra;
    }

    public void setEstoqueCompra(Integer estoqueCompra) {
        this.estoqueCompra = estoqueCompra;
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
                ", quantidade em estooque para reservas = " + estoqueReserva +
                ", quantidade em estoque para compra = " + estoqueCompra;
    }
}