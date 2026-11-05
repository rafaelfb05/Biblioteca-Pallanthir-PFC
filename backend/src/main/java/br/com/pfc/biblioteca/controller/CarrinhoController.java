package br.com.pfc.biblioteca.controller;

import br.com.pfc.biblioteca.dto.carrinho.AdicionarCarrinhoRequest;
import br.com.pfc.biblioteca.dto.carrinho.CarrinhoDTO;
import br.com.pfc.biblioteca.service.CarrinhoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/carrinho")
public class CarrinhoController {

    private final CarrinhoService service;

    public CarrinhoController(CarrinhoService service) {
        this.service = service;
    }

    @GetMapping
    public CarrinhoDTO obter(){
        return service.obter();
    }

    @PostMapping("/itens")
    public CarrinhoDTO adicionar(@RequestBody @Valid AdicionarCarrinhoRequest reqquest){
        return service.adicionar(reqquest);
    }

    @DeleteMapping("/itens/{livroId}")
    public CarrinhoDTO remover(@PathVariable Long livroId){
        return service.remover(livroId);
    }
}
