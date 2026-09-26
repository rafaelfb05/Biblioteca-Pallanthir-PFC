package br.com.pfc.biblioteca.config;

import br.com.pfc.biblioteca.enums.StatusReserva;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;
import java.util.stream.Collectors;

@Configuration
public class StatusReservaConstraintConfig {

    @Bean
    public ApplicationRunner atualizarConstraintStatusReserva(JdbcTemplate jdbcTemplate) {
        return args -> {
            String valores = Arrays.stream(StatusReserva.values())
                    .map(status -> "'" + status.name() + "'")
                    .collect(Collectors.joining(", "));

            jdbcTemplate.execute("ALTER TABLE reservas DROP CONSTRAINT IF EXISTS reservas_status_check");
            jdbcTemplate.execute("ALTER TABLE reservas ADD CONSTRAINT reservas_status_check CHECK (status IN (" + valores + "))");
        };
    }
}
