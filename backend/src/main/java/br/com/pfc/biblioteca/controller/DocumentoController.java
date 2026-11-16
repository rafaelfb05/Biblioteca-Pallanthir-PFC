package br.com.pfc.biblioteca.controller;

import br.com.pfc.biblioteca.dto.documento.AtualizarDocumentoRequest;
import br.com.pfc.biblioteca.dto.documento.DocumentoDTO;
import br.com.pfc.biblioteca.enums.TipoDocumento;
import br.com.pfc.biblioteca.service.DocumentoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/documentos")
public class DocumentoController {

    private final DocumentoService service;

    public DocumentoController(DocumentoService service) {
        this.service = service;
    }

    @GetMapping
    public List<DocumentoDTO> listar() {
        return service.listar();
    }

    @GetMapping("/{tipo}")
    public DocumentoDTO obter(@PathVariable TipoDocumento tipo) {
        return service.obter(tipo);
    }

    @PutMapping("/{tipo}")
    public DocumentoDTO atualizar(@PathVariable TipoDocumento tipo, @RequestBody @Valid AtualizarDocumentoRequest request) {
        return service.atualizar(tipo, request.conteudo());
    }
}
