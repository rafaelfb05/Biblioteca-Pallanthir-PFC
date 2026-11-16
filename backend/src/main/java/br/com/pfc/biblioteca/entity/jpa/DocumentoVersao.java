package br.com.pfc.biblioteca.entity.jpa;

import br.com.pfc.biblioteca.enums.TipoDocumento;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "documentos_versoes")
public class DocumentoVersao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Enumerated(EnumType.STRING)
    @Column(length = 40, nullable = false)
    private TipoDocumento tipo;
    @Column(nullable = false)
    private Integer numero;
    @Column(nullable = false, length = Documento.TAMANHO_MAXIMO)
    private String conteudo;
    private LocalDateTime publicadoEm;
    private String publicadoPorNome;
    private LocalDateTime arquivadoEm = LocalDateTime.now();

    public DocumentoVersao() {}

    public DocumentoVersao(Documento documento, int numero) {
        this.tipo = documento.getTipo();
        this.numero = numero;
        this.conteudo = documento.getConteudo();
        this.publicadoEm = documento.getAtualizadoEm();
        this.publicadoPorNome = documento.getAtualizadoPorNome();
    }

    public UUID getId() {
        return id;
    }

    public TipoDocumento getTipo() {
        return tipo;
    }

    public Integer getNumero() {
        return numero;
    }

    public String getConteudo() {
        return conteudo;
    }

    public LocalDateTime getPublicadoEm() {
        return publicadoEm;
    }

    public String getPublicadoPorNome() {
        return publicadoPorNome;
    }

    public LocalDateTime getArquivadoEm() {
        return arquivadoEm;
    }
}
