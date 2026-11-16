package br.com.pfc.biblioteca.controller;

import java.util.UUID;
import br.com.pfc.biblioteca.dto.reserva.DevolucaoRequest;
import br.com.pfc.biblioteca.dto.reserva.ReservaDTO;
import br.com.pfc.biblioteca.dto.reserva.ReservaGestaoDTO;
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
    public ResponseEntity<Void> reservar(@PathVariable UUID usuarioId, @PathVariable UUID livroId,
                                         @RequestParam(required = false) String politicaVersao){
        service.reservarLivro(usuarioId, livroId, politicaVersao);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{reservaId}/cancelar")
    public ResponseEntity<Void> cancelar(@PathVariable UUID reservaId){
        service.cancelarReserva(reservaId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{reservaId}/entregar")
    public ResponseEntity<Void> entregar(@PathVariable UUID reservaId){
        service.entregarLivro(reservaId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{reservaId}/devolver")
    public ResponseEntity<Void> devolver(@PathVariable UUID reservaId, @RequestBody(required = false) DevolucaoRequest request){
        service.devolverLivro(reservaId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/em-aberto")
    public List<ReservaGestaoDTO> listarEmAberto(){
        return service.listarReservasEmAberto();
    }

    @GetMapping("/usuario/{usuarioId}")
    public List<ReservaDTO> listarPorUsuario(@PathVariable UUID usuarioId){
        return service.listarReservasPorUsuario(usuarioId);
    }
}
