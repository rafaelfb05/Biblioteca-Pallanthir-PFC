package br.com.pfc.biblioteca.entity.jpa;

import br.com.pfc.biblioteca.enums.StatusMulta;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "multas")
public class Multa {

    public static final String PREFIXO_REFERENCIA = "multa-";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(optional = false)
    private Usuario usuario;
    @ManyToOne(optional = false)
    private Livro livro;
    @ManyToOne
    private Reserva reserva;
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal valor;
    @Column(length = 300)
    private String motivo;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusMulta status = StatusMulta.PENDENTE;
    private LocalDateTime dataCriacao = LocalDateTime.now();
    private LocalDateTime dataPagamento;
    private UUID registradaPorId;
    private String mpOrderId;
    private String linkPagamento;
    private LocalDateTime dataExpiracaoPagamento;

    public Multa() {}

    public Multa(Usuario usuario, Livro livro, Reserva reserva, BigDecimal valor, String motivo, UUID registradaPorId) {
        this.usuario = usuario;
        this.livro = livro;
        this.reserva = reserva;
        this.valor = valor;
        this.motivo = motivo;
        this.registradaPorId = registradaPorId;
    }

    public String getReferenciaDePagamento() {
        return PREFIXO_REFERENCIA + id;
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

    public Reserva getReserva() {
        return reserva;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public String getMotivo() {
        return motivo;
    }

    public StatusMulta getStatus() {
        return status;
    }

    public void setStatus(StatusMulta status) {
        this.status = status;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public LocalDateTime getDataPagamento() {
        return dataPagamento;
    }

    public void setDataPagamento(LocalDateTime dataPagamento) {
        this.dataPagamento = dataPagamento;
    }

    public UUID getRegistradaPorId() {
        return registradaPorId;
    }

    public String getMpOrderId() {
        return mpOrderId;
    }

    public void setMpOrderId(String mpOrderId) {
        this.mpOrderId = mpOrderId;
    }

    public String getLinkPagamento() {
        return linkPagamento;
    }

    public void setLinkPagamento(String linkPagamento) {
        this.linkPagamento = linkPagamento;
    }

    public LocalDateTime getDataExpiracaoPagamento() {
        return dataExpiracaoPagamento;
    }

    public void setDataExpiracaoPagamento(LocalDateTime dataExpiracaoPagamento) {
        this.dataExpiracaoPagamento = dataExpiracaoPagamento;
    }
}
