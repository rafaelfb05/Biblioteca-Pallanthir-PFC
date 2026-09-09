package br.com.pfc.biblioteca.controller;

import br.com.pfc.biblioteca.dto.LivroDTO;
import br.com.pfc.biblioteca.model.Livro;
import br.com.pfc.biblioteca.service.LivroService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pallanthir")
public class LivroController {

    @Autowired
    public LivroService service;

    @PostMapping("/cadastro")
    public ResponseEntity<LivroDTO> salvarLivro (@RequestBody String nomeLivro) {
        Livro livro = service.salvarLivro(nomeLivro);
        return ResponseEntity.ok(new LivroDTO(livro));
    }

}
