package br.com.pfc.biblioteca.infra.security;

import br.com.pfc.biblioteca.entity.jpa.Usuario;
import br.com.pfc.biblioteca.enums.TipoUsuario;
import br.com.pfc.biblioteca.repository.UsuarioRepository;
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
import java.util.UUID;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthFilter(JwtService jwtService, UsuarioRepository usuarioRepository){
        this.jwtService=jwtService;
        this.usuarioRepository=usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if(header != null && header.startsWith("Bearer ")){
            String token = header.substring(7);
            try{
                var claims = jwtService.validarToken(token);
                UUID id = UUID.fromString(claims.get("id", String.class));
                usuarioRepository.findById(id)
                        .filter(Usuario::isAtivo)
                        .filter(usuario -> usuario.getEmail().equals(claims.getSubject()))
                        .ifPresent(usuario -> SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(usuario.getEmail(), null, permissoes(usuario.getTipo()))));
            } catch (Exception e){
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private List<SimpleGrantedAuthority> permissoes(TipoUsuario tipo) {
        if (tipo == TipoUsuario.GERENTE) {
            return List.of(new SimpleGrantedAuthority("ROLE_GERENTE"), new SimpleGrantedAuthority("ROLE_FUNCIONARIO"));
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + tipo));
    }
}
