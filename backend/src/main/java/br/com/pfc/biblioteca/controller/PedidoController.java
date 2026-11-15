package br.com.pfc.biblioteca.controller;

import java.util.UUID;
import br.com.pfc.biblioteca.dto.pedido.PedidoDTO;
import br.com.pfc.biblioteca.dto.pedido.PedidoRetiradaDTO;
import br.com.pfc.biblioteca.service.PedidoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    private final PedidoService service;

    public PedidoController(PedidoService service) {
        this.service = service;
    }

    @PostMapping("/checkout")
    public ResponseEntity<PedidoDTO> checkout(){
        return ResponseEntity.ok(service.finalizarCompra());
    }

    @GetMapping
    public List<PedidoDTO> meusPedidos(){
        return service.meusPedidos();
    }

    @GetMapping("/{id}")
    public PedidoDTO obter(@PathVariable UUID id){
        return service.obterPedido(id);
    }

    @GetMapping("/pendentes-retirada")
    public List<PedidoRetiradaDTO> pendentesDeRetirada() {
        return service.pendentesDeRetirada();
    }

    @PutMapping("/{id}/entregar")
    public PedidoDTO entregar(@PathVariable UUID id) {
        return service.registrarEntrega(id);
    }

    @PostMapping("/{id}/reembolso")
    public PedidoDTO reembolsar(@PathVariable UUID id) {
        return service.solicitarReembolso(id);
    }
}
