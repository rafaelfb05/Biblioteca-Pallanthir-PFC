package br.com.pfc.biblioteca.dto;

import br.com.pfc.biblioteca.enums.TipoUsuario;
import br.com.pfc.biblioteca.entity.jpa.Usuario;

public record UsuarioDTO(Long id,
                         String nome,
                         String email,
                         TipoUsuario tipo) {

    public UsuarioDTO(Usuario usuario){
        this(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getTipo());
    }
}
