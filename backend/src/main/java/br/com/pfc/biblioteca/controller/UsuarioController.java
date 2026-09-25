package br.com.pfc.biblioteca.controller;

import br.com.pfc.biblioteca.dto.*;
import br.com.pfc.biblioteca.entity.Usuario;
import br.com.pfc.biblioteca.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @PostMapping("/cadastro")
    public ResponseEntity<UsuarioDTO> cadastrarUsuario(@RequestBody @Valid UsuarioRequest request) {
        Usuario usuario = service.cadastrarUsuario(request);
        return ResponseEntity.ok(new UsuarioDTO(usuario));
    }

    @GetMapping
    public List<UsuarioDTO> obterUsuarios() {
        return service.obterUsuarios();
    }

    @PutMapping("/atualizar/{id}")
    public ResponseEntity<UsuarioDTO> atualizarUsuario(@PathVariable Long id, @RequestBody UsuarioRequest request) {
        Usuario usuario = service.atualizarUsuario(id, request);
        return ResponseEntity.ok(new UsuarioDTO(usuario));
    }

    @DeleteMapping("/deletar/{id}")
    public ResponseEntity<Void> deletarUsuario(@PathVariable Long id) {
        service.excluirUsuario(id);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/{usuarioId}/favoritar/{livroId}")
    public ResponseEntity<Void> adicionarFavorito(@PathVariable Long usuarioId, @PathVariable Long livroId){
        service.adicionarFavorito(usuarioId, livroId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{usuarioId}/favoritos")
    public List<LivroDTO> listarFavoritos(@PathVariable Long usuarioId){
        return service.listarFavoritos(usuarioId);
    }

    @DeleteMapping("/{usuarioId}/favoritar/{livroId}")
    public ResponseEntity<Void> removerFavorito(@PathVariable Long usuarioId, @PathVariable Long livroId){
        service.removerFavorito(usuarioId, livroId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(@RequestBody LoginRequest request){
        service.login(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verificar-2fa")
    public ResponseEntity<LoginResponse> confirmar2FA(@RequestBody Confirmar2FARequest request){
        return ResponseEntity.ok(service.confirmar2FA(request));
    }

    @PostMapping("/recuperar-senha")
    public ResponseEntity<Void> recuperarSenha(@RequestBody SolicitarRecuperacaoRequest request){
        service.recuperacaoSenha(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/redefinir-senha")
    public ResponseEntity<Void> redefinirSenha(@RequestBody RedefinirSenhaRequest request){
        service.redefinirSenha(request);
        return ResponseEntity.ok().build();
    }
}