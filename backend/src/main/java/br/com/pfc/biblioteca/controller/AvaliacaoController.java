package br.com.pfc.biblioteca.controller;

import java.util.UUID;
import br.com.pfc.biblioteca.dto.avaliacao.AvaliacaoDTO;
import br.com.pfc.biblioteca.dto.avaliacao.AvaliacaoRequest;
import br.com.pfc.biblioteca.dto.livro.RankingLivroDTO;
import br.com.pfc.biblioteca.dto.avaliacao.ResumoAvaliacoesDTO;
import br.com.pfc.biblioteca.service.AvaliacaoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/avaliacoes")
public class AvaliacaoController {

    private final AvaliacaoService service;

    public AvaliacaoController(AvaliacaoService service) {
        this.service = service;
    }

    @PostMapping("/livro/{livroId}")
    public ResponseEntity<AvaliacaoDTO> avaliar(@PathVariable UUID livroId, @RequestBody @Valid AvaliacaoRequest request){
        return ResponseEntity.ok(service.avaliarLivro(livroId, request));
    }

    @GetMapping("/livro/{livroId}")
    public ResumoAvaliacoesDTO obterResumo(@PathVariable UUID livroId){
        return service.obterResumo(livroId);
    }

    @DeleteMapping("/livros/{livroId}")
    public ResponseEntity<Void> remover(@PathVariable UUID livroId){
        service.removerAvaliacao(livroId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/ranking")
    public List<RankingLivroDTO> ranking(){
        return service.obterRanking();
    }
}
