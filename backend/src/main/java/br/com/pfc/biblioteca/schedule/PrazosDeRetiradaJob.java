package br.com.pfc.biblioteca.schedule;

import br.com.pfc.biblioteca.service.PedidoService;
import br.com.pfc.biblioteca.service.ReservaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PrazosDeRetiradaJob {

    private static final Logger log = LoggerFactory.getLogger(PrazosDeRetiradaJob.class);

    private final ReservaService reservaService;
    private final PedidoService pedidoService;

    public PrazosDeRetiradaJob(ReservaService reservaService, PedidoService pedidoService) {
        this.reservaService = reservaService;
        this.pedidoService = pedidoService;
    }

    @Scheduled(initialDelay = 60_000, fixedDelay = 900_000)
    public void verificarPrazos() {
        for (UUID reservaId : reservaService.reservasComPrazoVencido()) {
            try {
                reservaService.cancelarPorPrazo(reservaId);
            } catch (RuntimeException e) {
                log.error("Não foi possível cancelar a reserva vencida {}: {}", reservaId, e.getMessage());
            }
        }
        for (UUID pedidoId : pedidoService.comprasComPrazoVencido()) {
            try {
                pedidoService.reembolsarPorPrazo(pedidoId);
            } catch (RuntimeException e) {
                log.error("Não foi possível reembolsar a compra não retirada {}: {}", pedidoId, e.getMessage());
            }
        }
    }
}
