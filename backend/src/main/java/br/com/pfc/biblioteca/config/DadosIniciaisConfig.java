package br.com.pfc.biblioteca.config;

import br.com.pfc.biblioteca.enums.TipoUsuario;
import br.com.pfc.biblioteca.service.DocumentoService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;
import java.util.stream.Collectors;

@Configuration
public class DadosIniciaisConfig {

    @Bean
    public ApplicationRunner atualizarConstraintTipoUsuario(JdbcTemplate jdbcTemplate) {
        return args -> {
            String valores = Arrays.stream(TipoUsuario.values())
                    .map(tipo -> "'" + tipo.name() + "'")
                    .collect(Collectors.joining(", "));

            jdbcTemplate.execute("ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS usuarios_tipo_check");
            jdbcTemplate.execute("ALTER TABLE usuarios ADD CONSTRAINT usuarios_tipo_check CHECK (tipo IN (" + valores + "))");
        };
    }

    @Bean
    public ApplicationRunner cadastrarDocumentosIniciais(DocumentoService documentoService) {
        return args -> documentoService.cadastrarDocumentosIniciais();
    }
}
