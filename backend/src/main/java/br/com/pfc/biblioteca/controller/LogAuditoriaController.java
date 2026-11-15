package br.com.pfc.biblioteca.controller;

import java.time.LocalDate;
import java.util.UUID;
import br.com.pfc.biblioteca.entity.mongo.LogAuditoria;
import br.com.pfc.biblioteca.service.LogAuditoriaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public List<LogAuditoria> listar(@RequestParam(required = false) UUID usuarioId,
                                     @RequestParam(required = false) String acao,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
                                     @RequestParam(required = false) Integer limite){
        return service.buscar(usuarioId, acao, de, ate, limite);
    }

    @GetMapping("/acoes")
    public List<String> listarAcoes(){
        return service.listarAcoes();
    }

    @GetMapping("/usuarios/{usuarioId}")
    public List<LogAuditoria> listarPorUsuario(@PathVariable UUID usuarioId) {
        return service.listarPorUsuario(usuarioId);
    }
}
