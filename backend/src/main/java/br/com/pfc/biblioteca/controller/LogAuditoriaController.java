package br.com.pfc.biblioteca.controller;

import br.com.pfc.biblioteca.entity.mongo.LogAuditoria;
import br.com.pfc.biblioteca.service.LogAuditoriaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/logs")
public class LogAuditoriaController {

    private final LogAuditoriaService service;

    public LogAuditoriaController(LogAuditoriaService service) {
        this.service = service;
    }

    @GetMapping
    public List<LogAuditoria> listarTodos(){
        return service.listarTodos();
    }

    @GetMapping("/usuarios/{usuarioId}")
    public List<LogAuditoria> listarPorUsuario(@PathVariable Long usuarioId) {
        return service.listarPorUsuario(usuarioId);
    }
}
