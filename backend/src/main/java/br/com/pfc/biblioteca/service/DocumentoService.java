package br.com.pfc.biblioteca.service;

import br.com.pfc.biblioteca.dto.documento.DocumentoDTO;
import br.com.pfc.biblioteca.entity.jpa.Documento;
import br.com.pfc.biblioteca.entity.jpa.Usuario;
import br.com.pfc.biblioteca.enums.TipoDocumento;
import br.com.pfc.biblioteca.infra.exception.RecursoNaoEncontradoException;
import br.com.pfc.biblioteca.repository.DocumentoRepository;
import br.com.pfc.biblioteca.repository.UsuarioRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class DocumentoService {

    private final DocumentoRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final LogAuditoriaService logService;

    public DocumentoService(DocumentoRepository repository, UsuarioRepository usuarioRepository,
                            LogAuditoriaService logService) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.logService = logService;
    }

    @Transactional
    public void cadastrarDocumentosIniciais() {
        for (TipoDocumento tipo : TipoDocumento.values()) {
            if (!repository.existsById(tipo)) {
                repository.save(new Documento(tipo, lerArquivoInicial(tipo)));
            }
        }
    }

    private String lerArquivoInicial(TipoDocumento tipo) {
        try (InputStream arquivo = new ClassPathResource("documentos/" + tipo.name() + ".md").getInputStream()) {
            return new String(arquivo.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Documento inicial não encontrado: " + tipo, e);
        }
    }

    @Transactional(readOnly = true)
    public List<DocumentoDTO> listar() {
        return Arrays.stream(TipoDocumento.values())
                .map(this::buscar)
                .map(DocumentoDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentoDTO obter(TipoDocumento tipo) {
        return new DocumentoDTO(buscar(tipo));
    }

    @Transactional
    public DocumentoDTO atualizar(TipoDocumento tipo, String conteudo) {
        Documento documento = buscar(tipo);
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario gerente = usuarioRepository.findByEmailAndAtivoTrue(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));

        String novo = normalizar(conteudo);
        if (novo.equals(normalizar(documento.getConteudo()))) {
            return new DocumentoDTO(documento);
        }

        documento.setConteudo(novo + "\n");
        documento.setAtualizadoEm(LocalDateTime.now());
        documento.setAtualizadoPorId(gerente.getId());
        documento.setAtualizadoPorNome(gerente.getNome());
        Documento salvo = repository.save(documento);
        logService.registrarLog(gerente.getId(), gerente.getEmail(), "DOCUMENTO_ATUALIZADO",
                tipo.getTitulo() + " atualizado pelo gerente");
        return new DocumentoDTO(salvo);
    }

    private String normalizar(String texto) {
        return texto.replace("\r\n", "\n").strip();
    }

    private Documento buscar(TipoDocumento tipo) {
        return repository.findById(tipo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Documento não encontrado"));
    }
}
