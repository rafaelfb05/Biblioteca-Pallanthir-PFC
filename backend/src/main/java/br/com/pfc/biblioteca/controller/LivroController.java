package br.com.pfc.biblioteca.controller;

import br.com.pfc.biblioteca.dto.AtualizarLivroRequest;
import br.com.pfc.biblioteca.dto.CadastroLivroRequest;
import br.com.pfc.biblioteca.dto.LivroDTO;
import br.com.pfc.biblioteca.dto.MateriaDTO;
import br.com.pfc.biblioteca.enums.Materia;
import br.com.pfc.biblioteca.model.Livro;
import br.com.pfc.biblioteca.service.LivroService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/livros")
public class LivroController {

    @Autowired
    public LivroService service;

    @PostMapping("/cadastro")
    public ResponseEntity<LivroDTO> salvarLivro(@RequestBody CadastroLivroRequest request) {
        Livro livro = service.salvarLivro(request.titulo(), request.materia(), request.preco(), request.estoque());
        return ResponseEntity.ok(new LivroDTO(livro));
    }

    @GetMapping
    public List<LivroDTO> obterLivros() {
        return service.obterTodosOsLivros();
    }

    @PutMapping("/atualizar/{id}")
    public ResponseEntity<LivroDTO> atualizarLivro(@PathVariable Long id, @RequestBody AtualizarLivroRequest request) {
        Livro livro = service.atualizarLivro(id, request);
        return ResponseEntity.ok(new LivroDTO(livro));
    }

    @DeleteMapping("/deletar/{id}")
    public ResponseEntity<Void> deletarLivro(@PathVariable Long id) {
        service.deletarLivro(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/materias")
    public List<MateriaDTO> listarMaterias() {
        return service.listarMaterias();
    }

    @GetMapping("/{materia}")
    public List<LivroDTO> filtarPorMateria(@PathVariable Materia materia) {
        return service.filtarPorMateria(materia);
    }

    @GetMapping("/titulo/{titulo}")
    public ResponseEntity<LivroDTO> buscarPorTitulo(@PathVariable String titulo) {
        return service.buscarPorNome(titulo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}