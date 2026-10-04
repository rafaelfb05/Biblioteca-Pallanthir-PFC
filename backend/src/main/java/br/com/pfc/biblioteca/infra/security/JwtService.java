package br.com.pfc.biblioteca.infra.security;

import br.com.pfc.biblioteca.entity.jpa.Usuario;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey chave = Keys.hmacShaKeyFor(System.getenv("JWT_SECRET").getBytes());

    public String gerarToken(Usuario usuario){
        return Jwts.builder()
                .subject(usuario.getEmail())
                .claim("id", usuario.getId())
                .claim("tipo", usuario.getTipo().name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 15))
                .signWith(chave)
                .compact();
    }

    public io.jsonwebtoken.Claims validarToken(String token){
        return Jwts.parser().verifyWith(chave).build()
                .parseSignedClaims(token).getPayload();
    }
}
