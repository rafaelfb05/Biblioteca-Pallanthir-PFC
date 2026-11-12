package br.com.pfc.biblioteca.entity.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "logs_auditoria")
public class LogAuditoria {
    @Id
    private String id;
    private String usuarioId;
    private String usuarioEmail;
    private String acao;
    private String descricao;
    private LocalDateTime dataHora;

    public LogAuditoria(){}

    public LogAuditoria(UUID usuarioId, String usuarioEmail, String acao, String descricao) {
        this.usuarioId = usuarioId == null ? null : usuarioId.toString();
        this.usuarioEmail = usuarioEmail;
        this.acao = acao;
        this.descricao = descricao;
        this.dataHora = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public String getUsuarioId() {
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
