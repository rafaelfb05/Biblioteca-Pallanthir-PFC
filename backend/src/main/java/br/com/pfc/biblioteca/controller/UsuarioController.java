package br.com.pfc.biblioteca.controller;

import br.com.pfc.biblioteca.dto.UsuarioDTO;
import br.com.pfc.biblioteca.dto.UsuarioRequest;
import br.com.pfc.biblioteca.model.Usuario;
import br.com.pfc.biblioteca.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    public UsuarioService service;

    @PostMapping("/cadastro")
    public ResponseEntity<UsuarioDTO> cadastrarUsuario(@RequestBody UsuarioRequest request) {
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
        service.deletarUsuario(id);
        return ResponseEntity.noContent().build();
    }
}