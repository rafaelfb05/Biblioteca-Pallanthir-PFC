package br.com.pfc.biblioteca.service;

import br.com.pfc.biblioteca.dto.UsuarioDTO;
import br.com.pfc.biblioteca.dto.UsuarioRequest;
import br.com.pfc.biblioteca.model.Livro;
import br.com.pfc.biblioteca.model.Usuario;
import br.com.pfc.biblioteca.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

public class UsuarioService {

    @Autowired
    private UsuarioRepository repository;

    private List<UsuarioDTO> converteDados(List<Usuario> usuario){
        return usuario.stream()
                .map(u -> new UsuarioDTO(
                        u.getId(), u.getNome(),
                        u.getEmail(), u.getSenha()))
                .collect(Collectors.toList());
    }

    public Usuario cadastrarUsuario(UsuarioRequest request) {
        Usuario usuario = new Usuario(request);
        return repository.save(usuario);
    }

    public List<UsuarioDTO> obterUsuarios() {
        return converteDados(repository.findAll());
    }
}
