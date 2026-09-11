package br.com.pfc.biblioteca.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.ExternalDocumentation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI pallanthirOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Biblioteca Pallanthir API")
                        .description("Back-end do sistema Pallanthir — biblioteca para estudantes, com avaliações, favoritos, reservas e compras.")
                        .version("0.0.1-SNAPSHOT")
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")))
                .externalDocs(new ExternalDocumentation()
                        .description("Repositório no GitHub")
                        .url("https://github.com/rafaelfb05/Biblioteca-Pallanthir-PFC"));
    }
}