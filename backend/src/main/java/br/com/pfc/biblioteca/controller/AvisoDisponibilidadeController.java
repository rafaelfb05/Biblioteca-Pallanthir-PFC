package br.com.pfc.biblioteca.controller;

import br.com.pfc.biblioteca.dto.aviso.AvisoDisponibilidadeDTO;
import br.com.pfc.biblioteca.dto.aviso.NovoAvisoRequest;
import br.com.pfc.biblioteca.service.AvisoDisponibilidadeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/avisos")
public class AvisoDisponibilidadeController {

    private final AvisoDisponibilidadeService service;

    public AvisoDisponibilidadeController(AvisoDisponibilidadeService service) {
        this.service = service;
    }

    @GetMapping("/meus")
    public List<AvisoDisponibilidadeDTO> meus() {
        return service.meusAvisos();
    }

    @PostMapping
    public AvisoDisponibilidadeDTO criar(@RequestBody @Valid NovoAvisoRequest request) {
        return service.criar(request.livroId(), request.tipo());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelar(@PathVariable UUID id) {
        service.cancelar(id);
        return ResponseEntity.noContent().build();
    }
}
