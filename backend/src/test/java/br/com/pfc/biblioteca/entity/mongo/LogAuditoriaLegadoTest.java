package br.com.pfc.biblioteca.entity.mongo;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LogAuditoriaLegadoTest {

    private MappingMongoConverter conversor() {
        MongoCustomConversions conversoes = new MongoCustomConversions(List.of());
        MongoMappingContext contexto = new MongoMappingContext();
        contexto.setSimpleTypeHolder(conversoes.getSimpleTypeHolder());
        contexto.afterPropertiesSet();
        MappingMongoConverter conversor = new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, contexto);
        conversor.setCustomConversions(conversoes);
        conversor.afterPropertiesSet();
        return conversor;
    }

    @Test
    void logAntigoComIdNumericoContinuaLegivel() {
        Document antigo = new Document("_id", new ObjectId())
                .append("usuarioId", 42L)
                .append("usuarioEmail", "estudante@teste.com")
                .append("acao", "SUCESSO_LOGIN")
                .append("descricao", "Código 2FA enviado");

        LogAuditoria log = conversor().read(LogAuditoria.class, antigo);

        assertThat(log.getUsuarioId()).isEqualTo("42");
        assertThat(log.getAcao()).isEqualTo("SUCESSO_LOGIN");
    }

    @Test
    void logNovoGuardaOUuidComoTexto() {
        UUID id = UUID.randomUUID();
        Document documento = new Document();

        conversor().write(new LogAuditoria(id, "estudante@teste.com", "SUCESSO_LOGIN", "Código 2FA enviado"), documento);

        assertThat(documento.get("usuarioId")).isEqualTo(id.toString());
    }
}
