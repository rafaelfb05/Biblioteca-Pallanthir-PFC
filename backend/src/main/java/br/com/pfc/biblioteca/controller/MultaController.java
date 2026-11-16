package br.com.pfc.biblioteca.controller;

import br.com.pfc.biblioteca.dto.multa.MultaDTO;
import br.com.pfc.biblioteca.service.MultaService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/multas")
public class MultaController {

    private final MultaService service;

    public MultaController(MultaService service) {
        this.service = service;
    }

    @GetMapping("/minhas")
    public List<MultaDTO> minhas() {
        return service.minhasMultas();
    }

    @PostMapping("/{id}/pagar")
    public MultaDTO pagar(@PathVariable UUID id) {
        return service.iniciarPagamento(id);
    }

    @GetMapping
    public List<MultaDTO> pendentes() {
        return service.listarPendentes();
    }

    @PutMapping("/{id}/dispensar")
    public MultaDTO dispensar(@PathVariable UUID id) {
        return service.dispensar(id);
    }
}
