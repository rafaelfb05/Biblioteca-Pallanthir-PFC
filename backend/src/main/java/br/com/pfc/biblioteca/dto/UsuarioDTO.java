package br.com.pfc.biblioteca.dto;

import br.com.pfc.biblioteca.enums.TipoUsuario;
import br.com.pfc.biblioteca.entity.Usuario;

public record UsuarioDTO(Long id,
                         String nome,
                         String email,
                         String senha,
                         TipoUsuario tipo) {

    public UsuarioDTO(Usuario usuario){
        this(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getSenha(), usuario.getTipo());
    }
}
