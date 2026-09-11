package br.com.pfc.biblioteca.service;

import br.com.pfc.biblioteca.dto.UsuarioDTO;
import br.com.pfc.biblioteca.dto.UsuarioRequest;
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

    public Usuario atualizarUsuario(Long id, UsuarioRequest request) {
        Usuario usuario = repository.findById(id).orElse(null);
        if(request.nome() != null){
            usuario.setNome(request.nome());
        }
        if(request.email() != null){
            usuario.setEmail(request.email());
        }
        if(request.senha() != null){
            usuario.setSenha(request.senha());
        }
        return repository.save(usuario);
    }

    public void deletarUsuario(Long id) {
        if(!repository.existsById(id)){
            throw new RuntimeException("livro não encontrado");
        }
        repository.deleteById(id);
    }
}