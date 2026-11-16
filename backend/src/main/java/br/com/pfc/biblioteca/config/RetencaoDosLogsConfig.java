package br.com.pfc.biblioteca.config;

import br.com.pfc.biblioteca.entity.mongo.LogAuditoria;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.concurrent.TimeUnit;

@Configuration
public class RetencaoDosLogsConfig {

    public static final long DIAS_DE_RETENCAO = 365;

    private static final Logger log = LoggerFactory.getLogger(RetencaoDosLogsConfig.class);

    @Bean
    public ApplicationRunner apagarLogsAntigosAutomaticamente(MongoTemplate mongoTemplate) {
        return args -> {
            try {
                mongoTemplate.getCollection(mongoTemplate.getCollectionName(LogAuditoria.class))
                        .createIndex(Indexes.ascending("dataHora"),
                                new IndexOptions().name("expiracao_dos_logs").expireAfter(DIAS_DE_RETENCAO, TimeUnit.DAYS));
            } catch (RuntimeException e) {
                log.warn("Não foi possível configurar a expiração dos logs: {}", e.getMessage());
            }
        };
    }
}
