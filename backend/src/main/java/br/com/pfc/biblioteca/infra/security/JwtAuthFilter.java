package br.com.pfc.biblioteca.infra.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService){
        this.jwtService=jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if(header != null && header.startsWith("Bearer ")){
            String token = header.substring(7);
            try{
                var claims = jwtService.validarToken(token);
                String tipo = claims.get("tipo", String.class);

                List<SimpleGrantedAuthority> permissoes = "GERENTE".equals(tipo)
                        ? List.of(new SimpleGrantedAuthority("ROLE_GERENTE"), new SimpleGrantedAuthority("ROLE_FUNCIONARIO"))
                        : List.of(new SimpleGrantedAuthority("ROLE_" + tipo));
                var auth = new UsernamePasswordAuthenticationToken(claims.getSubject(), null, permissoes);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception e){

            }
        }
        filterChain.doFilter(request, response);
    }
}
