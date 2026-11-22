package br.com.pfc.biblioteca.dto.log;

import br.com.pfc.biblioteca.entity.mongo.LogAuditoria;
import br.com.pfc.biblioteca.service.MascaraDeEmail;

import java.time.LocalDateTime;
import java.util.List;

public record LogAuditoriaDTO(String id, String usuarioId, String usuarioEmail, String acao,
                              String descricao, LocalDateTime dataHora, boolean emailOculto,
                              List<String> chavesDeEmail) {

    public static LogAuditoriaDTO oculto(LogAuditoria log) {
        boolean temEmail = MascaraDeEmail.contemEmail(log.getUsuarioEmail()) || MascaraDeEmail.contemEmail(log.getDescricao());
        return new LogAuditoriaDTO(log.getId(), log.getUsuarioId(), MascaraDeEmail.mascarar(log.getUsuarioEmail()),
                log.getAcao(), MascaraDeEmail.mascararNoTexto(log.getDescricao()), log.getDataHora(), temEmail,
                MascaraDeEmail.chavesDeBusca(log.getUsuarioEmail(), log.getDescricao()));
    }

    public static LogAuditoriaDTO completo(LogAuditoria log) {
        return new LogAuditoriaDTO(log.getId(), log.getUsuarioId(), log.getUsuarioEmail(), log.getAcao(),
                log.getDescricao(), log.getDataHora(), false, List.of());
    }
}
