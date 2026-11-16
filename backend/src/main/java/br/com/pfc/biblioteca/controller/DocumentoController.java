package br.com.pfc.biblioteca.controller;

import br.com.pfc.biblioteca.dto.documento.AtualizarDocumentoRequest;
import br.com.pfc.biblioteca.dto.documento.DocumentoDTO;
import br.com.pfc.biblioteca.dto.documento.DocumentoVersaoDTO;
import br.com.pfc.biblioteca.enums.TipoDocumento;
import br.com.pfc.biblioteca.service.DocumentoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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

    @GetMapping("/{tipo}/versoes")
    public List<DocumentoVersaoDTO> versoes(@PathVariable TipoDocumento tipo) {
        return service.versoesAnteriores(tipo);
    }

    @GetMapping("/versoes/{id}")
    public DocumentoVersaoDTO versao(@PathVariable UUID id) {
        return service.versao(id);
    }

    @PutMapping("/{tipo}")
    public DocumentoDTO atualizar(@PathVariable TipoDocumento tipo, @RequestBody @Valid AtualizarDocumentoRequest request) {
        return service.atualizar(tipo, request.conteudo());
    }
}
