package br.com.pfc.biblioteca.entity.jpa;

import br.com.pfc.biblioteca.enums.StatusAviso;
import br.com.pfc.biblioteca.enums.TipoEstoque;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "avisos_disponibilidade")
public class AvisoDisponibilidade {

    public static final int TENTATIVAS_MAXIMAS = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(optional = false)
    private Usuario usuario;
    @ManyToOne(optional = false)
    private Livro livro;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoEstoque tipo;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusAviso status = StatusAviso.PENDENTE;
    private LocalDateTime dataCriacao = LocalDateTime.now();
    private LocalDateTime dataEnvio;
    private Integer tentativas = 0;

    public AvisoDisponibilidade() {}

    public AvisoDisponibilidade(Usuario usuario, Livro livro, TipoEstoque tipo) {
        this.usuario = usuario;
        this.livro = livro;
        this.tipo = tipo;
    }

    public boolean estoqueDisponivel() {
        if (!livro.isAtivo()) {
            return false;
        }
        if (tipo == TipoEstoque.RESERVA) {
            return livro.getEstoqueReserva() != null && livro.getEstoqueReserva() > 0;
        }
        return livro.getEstoqueCompra() != null && livro.getEstoqueCompra() > 0
                && livro.getPreco() > 0;
    }

    public UUID getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Livro getLivro() {
        return livro;
    }

    public TipoEstoque getTipo() {
        return tipo;
    }

    public StatusAviso getStatus() {
        return status;
    }

    public void setStatus(StatusAviso status) {
        this.status = status;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public LocalDateTime getDataEnvio() {
        return dataEnvio;
    }

    public void setDataEnvio(LocalDateTime dataEnvio) {
        this.dataEnvio = dataEnvio;
    }

    public Integer getTentativas() {
        return tentativas;
    }

    public void setTentativas(Integer tentativas) {
        this.tentativas = tentativas;
    }
}
