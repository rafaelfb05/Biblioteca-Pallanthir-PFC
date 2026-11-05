package br.com.pfc.biblioteca.schedule;

import br.com.pfc.biblioteca.service.PedidoService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExpiracaoPedidosJob {

    private final PedidoService pedidoService;

    public ExpiracaoPedidosJob(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @Scheduled(fixedDelay = 60_000)
    public void expirar(){
        pedidoService.expirarPedidosVencidos();
    }
}
