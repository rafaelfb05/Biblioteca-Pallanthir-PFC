package br.com.pfc.biblioteca.service;

import br.com.pfc.biblioteca.entity.mongo.LogAuditoria;
import br.com.pfc.biblioteca.repository.LogAuditoriaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LogAuditoriaService {

    private final LogAuditoriaRepository repository;

    public LogAuditoriaService(LogAuditoriaRepository repository) {
        this.repository = repository;
    }

    public void registrarLog(Long usuarioId, String usuarioEmail, String acao, String descricao){
        LogAuditoria log = new LogAuditoria(usuarioId, usuarioEmail, acao, descricao);
        repository.save(log);
    }

    public List<LogAuditoria> listarTodos(){
        return repository.findAllByOrderByDataHoraDesc();
    }

    public List<LogAuditoria> listarPorUsuario(Long usuarioId){
        return repository.findByUsuarioIdOrderByDataHoraDesc(usuarioId);
    }
}
