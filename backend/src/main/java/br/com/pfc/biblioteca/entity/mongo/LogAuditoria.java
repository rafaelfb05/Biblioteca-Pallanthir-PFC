package br.com.pfc.biblioteca.entity.mongo;

import jakarta.persistence.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collation = "logs_auditoria")
public class LogAuditoria {
    @Id
    private String id;
    private Long usuarioId;
    private String usuarioEmail;
    private String acao;
    private String descricao;
    private LocalDateTime dataHora;

    public LogAuditoria(){}

    public LogAuditoria(Long usuarioId, String usuarioEmail, String descricao, String acao) {
        this.usuarioId = usuarioId;
        this.usuarioEmail = usuarioEmail;
        this.descricao = descricao;
        this.acao = acao;
        this.dataHora = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public String getUsuarioEmail() {
        return usuarioEmail;
    }

    public String getAcao() {
        return acao;
    }

    public String getDescricao() {
        return descricao;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }
}
