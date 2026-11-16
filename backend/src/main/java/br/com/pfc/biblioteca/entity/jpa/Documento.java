package br.com.pfc.biblioteca.entity.jpa;

import br.com.pfc.biblioteca.enums.TipoDocumento;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "documentos")
public class Documento {

    public static final int TAMANHO_MAXIMO = 100_000;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private TipoDocumento tipo;
    @Column(nullable = false, length = TAMANHO_MAXIMO)
    private String conteudo;
    private LocalDateTime atualizadoEm;
    private UUID atualizadoPorId;
    private String atualizadoPorNome;

    public Documento() {}

    public Documento(TipoDocumento tipo, String conteudo) {
        this.tipo = tipo;
        this.conteudo = conteudo;
    }

    public TipoDocumento getTipo() {
        return tipo;
    }

    public String getConteudo() {
        return conteudo;
    }

    public void setConteudo(String conteudo) {
        this.conteudo = conteudo;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(LocalDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }

    public UUID getAtualizadoPorId() {
        return atualizadoPorId;
    }

    public void setAtualizadoPorId(UUID atualizadoPorId) {
        this.atualizadoPorId = atualizadoPorId;
    }

    public String getAtualizadoPorNome() {
        return atualizadoPorNome;
    }

    public void setAtualizadoPorNome(String atualizadoPorNome) {
        this.atualizadoPorNome = atualizadoPorNome;
    }
}
