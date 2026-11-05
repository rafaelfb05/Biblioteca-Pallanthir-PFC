package br.com.pfc.biblioteca.controller;

import br.com.pfc.biblioteca.service.MercadoPagoService;
import br.com.pfc.biblioteca.service.PedidoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pagamentos")
public class PagamentoWebhookController {

    private final MercadoPagoService service;
    private final PedidoService pedidoService;

    public PagamentoWebhookController(MercadoPagoService service, PedidoService pedidoService) {
        this.service = service;
        this.pedidoService = pedidoService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> receber(
            @RequestHeader(value = "x-signature", required = false) String assinatura,
            @RequestHeader(value = "x-request-id", required = false) String requestId,
            @RequestParam(value = "data.id", required = false) String dataId) {

        if (!service.assinaturaValida(assinatura, requestId, dataId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        pedidoService.processarNotificacao(dataId);
        return ResponseEntity.ok().build();
    }
}
