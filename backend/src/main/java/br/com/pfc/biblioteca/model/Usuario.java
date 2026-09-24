package br.com.pfc.biblioteca.model;

import br.com.pfc.biblioteca.dto.UsuarioRequest;
import br.com.pfc.biblioteca.enums.TipoUsuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank
    private String nome;
    @Email
    @NotBlank
    private String email;
    @NotBlank
    private String senha;
    @Enumerated(EnumType.STRING)
    private TipoUsuario tipo;
    @ManyToMany
    private List<Livro> favoritos = new ArrayList<>();
    private boolean ativo = true;
    private int tentativasFalhas = 0;
    private LocalDateTime tempoBloqueio;

    public Usuario(){}

    public Usuario(UsuarioRequest request){
        this.nome=request.nome();
        this.email=request.email();
        this.senha=request.senha();
        this.tipo=request.tipo();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public TipoUsuario getTipo() {
        return tipo;
    }

    public void setTipo(TipoUsuario tipo) {
        this.tipo = tipo;
    }

    public List<Livro> getFavoritos() {
        return favoritos;
    }

    public void setFavoritos(List<Livro> favoritos) {
        this.favoritos = favoritos;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public int getTentativasFalhas() {
        return tentativasFalhas;
    }

    public void setTentativasFalhas(int tentativasFalhas) {
        this.tentativasFalhas = tentativasFalhas;
    }

    public LocalDateTime getTempoBloqueio() {
        return tempoBloqueio;
    }

    public void setTempoBloqueio(LocalDateTime tempoBloqueio) {
        this.tempoBloqueio = tempoBloqueio;
    }

    @Override
    public String toString() {
        return "Usuario = " + nome + '\'' +
                ", email = " + email + '\'' +
                ", senha = " + senha + '\'' +
                ", favoritos ="  + favoritos;
    }
}
