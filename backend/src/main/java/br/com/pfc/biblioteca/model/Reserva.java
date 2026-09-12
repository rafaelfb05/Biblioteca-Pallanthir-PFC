package br.com.pfc.biblioteca.model;

import br.com.pfc.biblioteca.enums.StatusReserva;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservas")
public class Reserva {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    private Usuario usuario;
    @ManyToOne
    private Livro livro;
    private LocalDateTime dataReserva;
    @Enumerated(EnumType.STRING)
    private StatusReserva status;

    public Reserva(){}

    public Reserva (Usuario usuario, Livro livro){
        this.usuario=usuario;
        this.livro=livro;
        this.dataReserva=LocalDateTime.now();
        this.status=StatusReserva.RESERVADO;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public LocalDateTime getDataReserva() {
        return dataReserva;
    }

    public void setDataReserva(LocalDateTime dataReserva) {
        this.dataReserva = dataReserva;
    }

    public StatusReserva getStatus() {
        return status;
    }

    public void setStatus(StatusReserva status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Usuario = " + usuario +
                ", livro =" + livro +
                ", dataReserva = " + dataReserva +
                ", status = " + status;
    }
}
