package br.com.pfc.biblioteca.controller;

import br.com.pfc.biblioteca.dto.ReservaDTO;
import br.com.pfc.biblioteca.service.ReservaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reservas")
public class ReservaController {

    private final ReservaService service;

    public ReservaController(ReservaService service) {
        this.service = service;
    }

    @PostMapping("/{usuarioId}/{livroId}")
    public ResponseEntity<Void> reservar(@PathVariable Long usuarioId, @PathVariable Long livroId){
        service.reservarLivro(usuarioId, livroId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{reservaId}/cancelar")
    public ResponseEntity<Void> cancelar(@PathVariable Long reservaId){
        service.cancelarReserva(reservaId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{reservaId}/entregar")
    public ResponseEntity<Void> entregar(@PathVariable Long reservaId){
        service.entregarLivro(reservaId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{reservaId}/devolver")
    public ResponseEntity<Void> devolver(@PathVariable Long reservaId){
        service.devolverLivro(reservaId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/usuario/{usuarioId}")
    public List<ReservaDTO> listarPorUsuario(@PathVariable Long usuarioId){
        return service.listarReservasPorUsuario(usuarioId);
    }
}
