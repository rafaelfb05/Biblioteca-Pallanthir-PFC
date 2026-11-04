package br.com.pfc.biblioteca.entity.jpa;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "itens_pedido")
public class ItemPedido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    private Pedido pedido;
    @ManyToOne(optional = false)
    private Livro livro;
    private Integer quantidade;
    @Column(precision = 10, scale = 2)
    private BigDecimal precoUnitario;

    public ItemPedido(){}

    public ItemPedido(Pedido pedido, Livro livro, Integer quantidade, BigDecimal precoUnitario){
        this.pedido=pedido;
        this.livro=livro;
        this.quantidade=quantidade;
        this.precoUnitario=precoUnitario;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public void setPedido(Pedido pedido) {
        this.pedido = pedido;
    }

    public Livro getLivro() {
        return livro;
    }

    public void setLivro(Livro livro) {
        this.livro = livro;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public void setPrecoUnitario(BigDecimal precoUnitario) {
        this.precoUnitario = precoUnitario;
    }
}
