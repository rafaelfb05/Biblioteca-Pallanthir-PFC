package br.com.pfc.biblioteca.model;

import br.com.pfc.biblioteca.dto.UsuarioRequest;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

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
    @Column(unique = true)
    private String email;
    @NotBlank
    private String senha;
    @ManyToMany(fetch = FetchType.EAGER)
    private List<Livro> favoritos = new ArrayList<>();

    public Usuario(){}

    public Usuario(UsuarioRequest request){
        this.nome= request.nome();
        this.email= request.email();
        this.senha= request.senha();
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

    public List<Livro> getFavoritos() {
        return favoritos;
    }

    public void setFavoritos(List<Livro> favoritos) {
        this.favoritos = favoritos;
    }

    @Override
    public String toString() {
        return "Usuario = " + nome + '\'' +
                ", email = " + email + '\'' +
                ", senha = " + senha + '\'' +
                ", favoritos ="  + favoritos;
    }
}
