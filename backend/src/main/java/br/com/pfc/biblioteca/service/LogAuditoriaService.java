package br.com.pfc.biblioteca.service;

import br.com.pfc.biblioteca.entity.jpa.Usuario;
import br.com.pfc.biblioteca.entity.mongo.LogAuditoria;
import br.com.pfc.biblioteca.repository.LogAuditoriaRepository;
import br.com.pfc.biblioteca.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class LogAuditoriaService {

    private static final Logger logger = LoggerFactory.getLogger(LogAuditoriaService.class);

    private final LogAuditoriaRepository repository;
    private final UsuarioRepository usuarioRepository;

    public LogAuditoriaService(LogAuditoriaRepository repository, UsuarioRepository usuarioRepository) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
    }

    public void registrarLog(Long usuarioId, String usuarioEmail, String acao, String descricao){
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
        Long usuarioId = emailLogado == null ? null : usuarioRepository.findByEmailAndAtivoTrue(emailLogado)
                .map(Usuario::getId)
                .orElse(null);
        registrarLog(usuarioId, emailLogado, acao, descricao);
    }

    public List<LogAuditoria> listarTodos(){
        return repository.findAllByOrderByDataHoraDesc();
    }

    public List<LogAuditoria> listarPorUsuario(Long usuarioId){
        return repository.findByUsuarioIdOrderByDataHoraDesc(usuarioId);
    }
}
