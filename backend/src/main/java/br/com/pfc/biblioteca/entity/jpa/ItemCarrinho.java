package br.com.pfc.biblioteca.entity.jpa;

import java.util.UUID;
import jakarta.persistence.*;

@Entity
@Table(name = "itens_carrinho", uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "livro_id"}))
public class ItemCarrinho {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(optional = false)
    private Usuario usuario;
    @ManyToOne(optional = false)
    private Livro livro;
    private Integer quantidade;
    private boolean ativo = true;

    public ItemCarrinho(){}

    public ItemCarrinho(Usuario usuario, Livro livro){
        this.usuario=usuario;
        this.livro=livro;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
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

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
