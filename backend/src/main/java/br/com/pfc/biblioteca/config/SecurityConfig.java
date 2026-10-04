package br.com.pfc.biblioteca.config;

import br.com.pfc.biblioteca.infra.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter){
        this.jwtAuthFilter=jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/usuarios/cadastro", "/usuarios/login",
                               "/usuarios/verificar-2fa", "/usuarios/recuperar-senha", "/usuarios/redefinir-senha").permitAll()
                        .requestMatchers(HttpMethod.GET, "/livros/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/logs/**").hasRole("FUNCIONARIO")
                        .requestMatchers(HttpMethod.POST, "/livros/cadastro").hasRole("FUNCIONARIO")
                        .requestMatchers(HttpMethod.PUT, "/livros/atualizar/*").hasRole("FUNCIONARIO")
                        .requestMatchers(HttpMethod.DELETE, "/livros/deletar/*").hasRole("FUNCIONARIO")
                        .requestMatchers(HttpMethod.PUT, "/reservas/*/devolver").hasRole("FUNCIONARIO")
                        .requestMatchers(HttpMethod.PUT, "/reservas/*/entregar").hasRole("FUNCIONARIO")
                        .requestMatchers(HttpMethod.GET, "/usuarios").hasRole("FUNCIONARIO")
                        .requestMatchers(HttpMethod.POST, "/usuarios/*/favoritar/*").hasRole("ESTUDANTE")
                        .requestMatchers(HttpMethod.DELETE, "/usuarios/*/favoritar/*").hasRole("ESTUDANTE")
                        .requestMatchers(HttpMethod.POST, "/reservas/*/*").hasRole("ESTUDANTE")
                        .requestMatchers(HttpMethod.PUT, "/reservas/*/cancelar").hasRole("ESTUDANTE")
                        .requestMatchers(HttpMethod.GET, "/reservas/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/usuarios/deletar/*").authenticated()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
