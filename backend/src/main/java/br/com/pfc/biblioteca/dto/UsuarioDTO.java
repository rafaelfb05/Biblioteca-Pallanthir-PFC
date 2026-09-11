package br.com.pfc.biblioteca.dto;

import br.com.pfc.biblioteca.model.Usuario;

public record UsuarioDTO(Long id,
                         String nome,
                         String email,
                         String senha) {

    public UsuarioDTO(Usuario usuario){
        this(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getSenha());
    }
}
