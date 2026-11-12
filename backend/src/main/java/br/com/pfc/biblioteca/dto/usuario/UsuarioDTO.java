package br.com.pfc.biblioteca.dto.usuario;

import java.util.UUID;
import br.com.pfc.biblioteca.enums.TipoUsuario;
import br.com.pfc.biblioteca.entity.jpa.Usuario;

public record UsuarioDTO(UUID id,
                         String nome,
                         String email,
                         TipoUsuario tipo) {

    public UsuarioDTO(Usuario usuario){
        this(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getTipo());
    }
}
