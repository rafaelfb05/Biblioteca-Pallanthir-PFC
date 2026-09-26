package br.com.pfc.biblioteca.entity.jpa;

import br.com.pfc.biblioteca.dto.UsuarioRequest;
import br.com.pfc.biblioteca.enums.TipoUsuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.ColumnDefault;

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
    @Column(nullable = false)
    @ColumnDefault("true")
    private boolean ativo = true;
    private Integer tentativasFalhas = 0;
    private LocalDateTime tempoBloqueio;
    private String codigoRecuperacao;
    private LocalDateTime expiracaoCodigo;
    private String codigo2FA;
    private LocalDateTime expiracao2FA;
    private LocalDateTime dataAceiteTermos;

    public Usuario(){}

    public Usuario(UsuarioRequest request){
        this.nome=request.nome();
        this.email=request.email();
        this.senha=request.senha();
        this.tipo=request.tipo();
    }

    public LocalDateTime getDataAceiteTermos() {
        return dataAceiteTermos;
    }

    public void setDataAceiteTermos(LocalDateTime dataAceiteTermos) {
        this.dataAceiteTermos = dataAceiteTermos;
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

    public Integer getTentativasFalhas() {
        return tentativasFalhas;
    }

    public void setTentativasFalhas(Integer tentativasFalhas) {
        this.tentativasFalhas = tentativasFalhas;
    }

    public LocalDateTime getTempoBloqueio() {
        return tempoBloqueio;
    }

    public void setTempoBloqueio(LocalDateTime tempoBloqueio) {
        this.tempoBloqueio = tempoBloqueio;
    }

    public String getCodigoRecuperacao() {
        return codigoRecuperacao;
    }

    public void setCodigoRecuperacao(String codigoRecuperacao) {
        this.codigoRecuperacao = codigoRecuperacao;
    }

    public LocalDateTime getExpiracaoCodigo() {
        return expiracaoCodigo;
    }

    public void setExpiracaoCodigo(LocalDateTime expiracaoCodigo) {
        this.expiracaoCodigo = expiracaoCodigo;
    }

    public String getCodigo2FA() {
        return codigo2FA;
    }

    public void setCodigo2FA(String codigo2FA) {
        this.codigo2FA = codigo2FA;
    }

    public LocalDateTime getExpiracao2FA() {
        return expiracao2FA;
    }

    public void setExpiracao2FA(LocalDateTime expiracao2FA) {
        this.expiracao2FA = expiracao2FA;
    }

    @Override
    public String toString() {
        return "Usuario = " + nome + '\'' +
                ", email = " + email + '\'' +
                ", senha = " + senha + '\'' +
                ", favoritos ="  + favoritos;
    }
}
