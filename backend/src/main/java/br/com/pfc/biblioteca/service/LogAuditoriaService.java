package br.com.pfc.biblioteca.service;

import java.util.UUID;
import br.com.pfc.biblioteca.entity.jpa.Usuario;
import br.com.pfc.biblioteca.entity.mongo.LogAuditoria;
import br.com.pfc.biblioteca.infra.exception.RecursoNaoEncontradoException;
import br.com.pfc.biblioteca.repository.LogAuditoriaRepository;
import br.com.pfc.biblioteca.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class LogAuditoriaService {

    private static final Logger logger = LoggerFactory.getLogger(LogAuditoriaService.class);

    public static final int LIMITE_PADRAO = 500;
    public static final int LIMITE_MAXIMO = 1000;

    private final LogAuditoriaRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final MongoTemplate mongoTemplate;

    public LogAuditoriaService(LogAuditoriaRepository repository, UsuarioRepository usuarioRepository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public void registrarLog(UUID usuarioId, String usuarioEmail, String acao, String descricao){
        LogAuditoria log = new LogAuditoria(usuarioId, usuarioEmail, acao, descricao);
        CompletableFuture.runAsync(() -> repository.save(log))
                .exceptionally(erro -> {
                    logger.error("Não foi possível gravar o log de auditoria {}: {}", acao, erro.getMessage());
                    return null;
                });
    }

    public void registrarLogUsuarioLogado(String acao, String descricao){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String emailLogado = auth != null ? auth.getName() : null;
        UUID usuarioId = emailLogado == null ? null : usuarioRepository.findByEmailAndAtivoTrue(emailLogado)
                .map(Usuario::getId)
                .orElse(null);
        registrarLog(usuarioId, emailLogado, acao, descricao);
    }

    public List<LogAuditoria> listarTodos(){
        return buscar(null, null, null, null, LIMITE_PADRAO);
    }

    public List<LogAuditoria> buscar(UUID usuarioId, String acao, LocalDate de, LocalDate ate, Integer limite) {
        Query consulta = new Query();
        if (usuarioId != null) {
            consulta.addCriteria(Criteria.where("usuarioId").is(usuarioId.toString()));
        }
        if (acao != null && !acao.isBlank()) {
            consulta.addCriteria(Criteria.where("acao").is(acao.trim()));
        }
        if (de != null || ate != null) {
            Criteria periodo = Criteria.where("dataHora");
            if (de != null) {
                periodo = periodo.gte(de.atStartOfDay());
            }
            if (ate != null) {
                periodo = periodo.lt(ate.plusDays(1).atStartOfDay());
            }
            consulta.addCriteria(periodo);
        }
        int quantidade = limite == null ? LIMITE_PADRAO : Math.max(1, Math.min(limite, LIMITE_MAXIMO));
        consulta.with(Sort.by(Sort.Direction.DESC, "dataHora")).limit(quantidade);
        return mongoTemplate.find(consulta, LogAuditoria.class);
    }

    public List<String> listarAcoes() {
        return mongoTemplate.findDistinct(new Query(), "acao", LogAuditoria.class, String.class)
                .stream()
                .filter(acao -> acao != null && !acao.isBlank())
                .sorted()
                .toList();
    }

    public List<LogAuditoria> listarPorUsuario(UUID usuarioId){
        return buscar(usuarioId, null, null, null, LIMITE_PADRAO);
    }

    public LogAuditoria revelarEmail(String logId) {
        LogAuditoria log = repository.findById(logId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Registro de log não encontrado"));
        String dono = nomeDoUsuario(log.getUsuarioId());
        String descricao = "Visualizou o e-mail do registro " + log.getAcao()
                + (dono == null ? "" : " de " + dono)
                + " (log " + log.getId() + ")";
        registrarLogUsuarioLogado("VISUALIZOU_EMAIL", descricao);
        return log;
    }

    private String nomeDoUsuario(String usuarioId) {
        if (usuarioId == null) {
            return null;
        }
        try {
            return usuarioRepository.findById(UUID.fromString(usuarioId)).map(Usuario::getNome).orElse(null);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
