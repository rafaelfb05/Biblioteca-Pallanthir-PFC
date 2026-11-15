package br.com.pfc.biblioteca.service;

import br.com.pfc.biblioteca.entity.mongo.LogAuditoria;
import br.com.pfc.biblioteca.repository.LogAuditoriaRepository;
import br.com.pfc.biblioteca.repository.UsuarioRepository;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LogAuditoriaServiceTest {

    private final MongoTemplate mongoTemplate = mock(MongoTemplate.class);
    private final LogAuditoriaService service = new LogAuditoriaService(
            mock(LogAuditoriaRepository.class), mock(UsuarioRepository.class), mongoTemplate);

    private Query consultaExecutada() {
        ArgumentCaptor<Query> consulta = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(consulta.capture(), eq(LogAuditoria.class));
        return consulta.getValue();
    }

    @Test
    void semFiltrosTrazOsMaisRecentesAteOLimitePadrao() {
        service.listarTodos();

        Query consulta = consultaExecutada();
        assertThat(consulta.getQueryObject()).isEmpty();
        assertThat(consulta.getLimit()).isEqualTo(LogAuditoriaService.LIMITE_PADRAO);
        assertThat(consulta.getSortObject()).isEqualTo(new Document("dataHora", -1));
    }

    @Test
    void filtrosDeUsuarioAcaoEPeriodoVaoParaOBanco() {
        UUID usuario = UUID.randomUUID();

        service.buscar(usuario, "SUCESSO_LOGIN", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 15), 50);

        Document filtro = consultaExecutada().getQueryObject();
        assertThat(filtro.get("usuarioId")).isEqualTo(usuario.toString());
        assertThat(filtro.get("acao")).isEqualTo("SUCESSO_LOGIN");
        Document periodo = (Document) filtro.get("dataHora");
        assertThat(periodo.get("$gte")).isEqualTo(LocalDateTime.of(2026, 11, 1, 0, 0));
        assertThat(periodo.get("$lt")).isEqualTo(LocalDateTime.of(2026, 11, 16, 0, 0));
    }

    @Test
    void limiteNuncaPassaDoMaximo() {
        service.buscar(null, null, null, null, 100000);

        assertThat(consultaExecutada().getLimit()).isEqualTo(LogAuditoriaService.LIMITE_MAXIMO);
    }

    @Test
    void acoesVemOrdenadasESemVazios() {
        when(mongoTemplate.findDistinct(any(Query.class), eq("acao"), eq(LogAuditoria.class), eq(String.class)))
                .thenReturn(List.of("SUCESSO_LOGIN", "", "ACEITE_TERMOS"));

        assertThat(service.listarAcoes()).containsExactly("ACEITE_TERMOS", "SUCESSO_LOGIN");
    }
}
