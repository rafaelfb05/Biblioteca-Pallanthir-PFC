package br.com.pfc.biblioteca.service;

import br.com.pfc.biblioteca.dto.LivroDTO;
import br.com.pfc.biblioteca.dto.UsuarioDTO;
import br.com.pfc.biblioteca.dto.UsuarioRequest;
import br.com.pfc.biblioteca.model.Livro;
import br.com.pfc.biblioteca.model.Usuario;
import br.com.pfc.biblioteca.repository.LivroRepository;
import br.com.pfc.biblioteca.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository repository;
    @Autowired
    private LivroRepository livroRepository;

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
    @Transactional
    public void adicionarFavorito(Long usuarioId, Long livroId){
        Usuario usuario = repository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        Livro livro = livroRepository.findById(livroId)
                .orElseThrow(() -> new RuntimeException("Livro não encontrado!"));

        // Livro nao sobrescreve equals/hashCode, entao contains() comparava por
        // referencia e deixava o mesmo livro ser favoritado varias vezes.
        boolean jaFavoritado = usuario.getFavoritos().stream()
                .anyMatch(l -> l.getId().equals(livro.getId()));

        if (!jaFavoritado) {
            usuario.getFavoritos().add(livro);
            repository.save(usuario);
        }
    }

    @Transactional
    public void removerFavorito(Long usuarioId, Long livroId) {
        Usuario usuario = repository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        usuario.getFavoritos().removeIf(l -> l.getId().equals(livroId));
        repository.save(usuario);

    }

    public List<LivroDTO> listarFavoritos(Long usuarioId) {
        Usuario usuario = repository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        return usuario.getFavoritos().stream()
                .map(LivroDTO::new)
                .collect(Collectors.toList());
    }
}
