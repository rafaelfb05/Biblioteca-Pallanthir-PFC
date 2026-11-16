package br.com.pfc.biblioteca.service;

import br.com.pfc.biblioteca.infra.exception.ConflitoException;
import br.com.pfc.biblioteca.dto.documento.DocumentoDTO;
import br.com.pfc.biblioteca.dto.documento.DocumentoVersaoDTO;
import br.com.pfc.biblioteca.entity.jpa.Documento;
import br.com.pfc.biblioteca.entity.jpa.DocumentoVersao;
import br.com.pfc.biblioteca.entity.jpa.Usuario;
import br.com.pfc.biblioteca.enums.TipoDocumento;
import br.com.pfc.biblioteca.infra.exception.RecursoNaoEncontradoException;
import br.com.pfc.biblioteca.repository.DocumentoRepository;
import br.com.pfc.biblioteca.repository.DocumentoVersaoRepository;
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
import java.util.UUID;

@Service
public class DocumentoService {

    private final DocumentoRepository repository;
    private final DocumentoVersaoRepository versaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final LogAuditoriaService logService;

    public DocumentoService(DocumentoRepository repository, DocumentoVersaoRepository versaoRepository,
                            UsuarioRepository usuarioRepository, LogAuditoriaService logService) {
        this.repository = repository;
        this.versaoRepository = versaoRepository;
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
                .map(this::paraDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentoDTO obter(TipoDocumento tipo) {
        return paraDTO(buscar(tipo));
    }

    @Transactional(readOnly = true)
    public String versaoVigenteDosTermos() {
        return VersaoDeDocumentos.calcular(buscar(TipoDocumento.TERMOS_DE_USO).getConteudo(),
                buscar(TipoDocumento.POLITICA_DE_PRIVACIDADE).getConteudo());
    }

    @Transactional(readOnly = true)
    public String versaoVigenteDaPoliticaDeReservas() {
        return VersaoDeDocumentos.calcular(buscar(TipoDocumento.POLITICA_DE_RESERVAS).getConteudo());
    }

    public void garantirTermosAceitos(Usuario usuario) {
        if (!versaoVigenteDosTermos().equals(usuario.getVersaoTermos())) {
            throw new ConflitoException("Os Termos de Uso ou a Política de Privacidade foram atualizados. "
                    + "Aceite a nova versão para continuar.");
        }
    }

    @Transactional(readOnly = true)
    public List<DocumentoVersaoDTO> versoesAnteriores(TipoDocumento tipo) {
        return versaoRepository.findByTipoOrderByNumeroDesc(tipo).stream()
                .map(DocumentoVersaoDTO::resumo)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentoVersaoDTO versao(UUID id) {
        return versaoRepository.findById(id)
                .map(DocumentoVersaoDTO::completo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Versão não encontrada"));
    }

    @Transactional
    public DocumentoDTO atualizar(TipoDocumento tipo, String conteudo) {
        Documento documento = buscar(tipo);
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario gerente = usuarioRepository.findByEmailAndAtivoTrue(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));

        String novo = normalizar(conteudo);
        if (novo.equals(normalizar(documento.getConteudo()))) {
            return paraDTO(documento);
        }

        long versaoArquivada = versaoRepository.countByTipo(tipo) + 1;
        versaoRepository.save(new DocumentoVersao(documento, (int) versaoArquivada));

        documento.setConteudo(novo + "\n");
        documento.setAtualizadoEm(LocalDateTime.now());
        documento.setAtualizadoPorId(gerente.getId());
        documento.setAtualizadoPorNome(gerente.getNome());
        Documento salvo = repository.save(documento);
        logService.registrarLog(gerente.getId(), gerente.getEmail(), "DOCUMENTO_ATUALIZADO",
                tipo.getTitulo() + " atualizado pelo gerente (versão " + (versaoArquivada + 1)
                        + "; a versão " + versaoArquivada + " foi guardada no histórico)");
        return paraDTO(salvo);
    }

    private DocumentoDTO paraDTO(Documento documento) {
        return new DocumentoDTO(documento, versaoRepository.countByTipo(documento.getTipo()) + 1);
    }

    private String normalizar(String texto) {
        return texto.replace("\r\n", "\n").strip();
    }

    private Documento buscar(TipoDocumento tipo) {
        return repository.findById(tipo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Documento não encontrado"));
    }
}
