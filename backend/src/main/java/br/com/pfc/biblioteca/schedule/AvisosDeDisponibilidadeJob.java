package br.com.pfc.biblioteca.schedule;

import br.com.pfc.biblioteca.service.AvisoDisponibilidadeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AvisosDeDisponibilidadeJob {

    private static final Logger log = LoggerFactory.getLogger(AvisosDeDisponibilidadeJob.class);

    private final AvisoDisponibilidadeService service;

    public AvisosDeDisponibilidadeJob(AvisoDisponibilidadeService service) {
        this.service = service;
    }

    @Scheduled(initialDelay = 90_000, fixedDelay = 120_000)
    public void enviarAvisos() {
        for (UUID avisoId : service.avisosProntosParaEnvio()) {
            try {
                service.enviar(avisoId);
            } catch (RuntimeException e) {
                log.error("Não foi possível processar o aviso de disponibilidade {}: {}", avisoId, e.getMessage());
            }
        }
    }
}
