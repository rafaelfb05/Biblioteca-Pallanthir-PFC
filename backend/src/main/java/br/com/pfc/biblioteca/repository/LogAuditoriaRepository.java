package br.com.pfc.biblioteca.repository;

import br.com.pfc.biblioteca.entity.mongo.LogAuditoria;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface LogAuditoriaRepository extends MongoRepository <LogAuditoria, String> {

    List<LogAuditoria> findAllByOrderByDataHoraDesc();

    List<LogAuditoria> findByUsuarioIdOrderByDataHoraDesc(Long usuarioId);
}
