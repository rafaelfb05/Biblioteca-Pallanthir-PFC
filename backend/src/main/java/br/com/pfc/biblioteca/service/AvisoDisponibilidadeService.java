package br.com.pfc.biblioteca.service;

import br.com.pfc.biblioteca.dto.aviso.AvisoDisponibilidadeDTO;
import br.com.pfc.biblioteca.entity.jpa.AvisoDisponibilidade;
import br.com.pfc.biblioteca.entity.jpa.Livro;
import br.com.pfc.biblioteca.entity.jpa.Usuario;
import br.com.pfc.biblioteca.enums.StatusAviso;
import br.com.pfc.biblioteca.enums.TipoEstoque;
import br.com.pfc.biblioteca.infra.exception.ConflitoException;
import br.com.pfc.biblioteca.infra.exception.RecursoNaoEncontradoException;
import br.com.pfc.biblioteca.infra.exception.RegraDeNegocioException;
import br.com.pfc.biblioteca.repository.AvisoDisponibilidadeRepository;
import br.com.pfc.biblioteca.repository.LivroRepository;
import br.com.pfc.biblioteca.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AvisoDisponibilidadeService {

    private static final Logger logger = LoggerFactory.getLogger(AvisoDisponibilidadeService.class);

    private final AvisoDisponibilidadeRepository repository;
    private final LivroRepository livroRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;
    private final LogAuditoriaService logService;

    public AvisoDisponibilidadeService(AvisoDisponibilidadeRepository repository, LivroRepository livroRepository,
                                       UsuarioRepository usuarioRepository, EmailService emailService,
                                       LogAuditoriaService logService) {
        this.repository = repository;
        this.livroRepository = livroRepository;
        this.usuarioRepository = usuarioRepository;
        this.emailService = emailService;
        this.logService = logService;
    }

    private Usuario usuarioLogado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmailAndAtivoTrue(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
    }

    @Transactional(readOnly = true)
    public List<AvisoDisponibilidadeDTO> meusAvisos() {
        return repository.findByUsuarioIdAndStatusOrderByDataCriacaoDesc(usuarioLogado().getId(), StatusAviso.PENDENTE)
                .stream()
                .map(AvisoDisponibilidadeDTO::new)
                .toList();
    }

    @Transactional
    public AvisoDisponibilidadeDTO criar(UUID livroId, TipoEstoque tipo) {
        Usuario usuario = usuarioLogado();
        Livro livro = livroRepository.findById(livroId)
                .filter(Livro::isAtivo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Livro não encontrado"));

        if (tipo == TipoEstoque.COMPRA && livro.getPreco() <= 0) {
            throw new RegraDeNegocioException("Este livro não está à venda");
        }

        AvisoDisponibilidade novo = new AvisoDisponibilidade(usuario, livro, tipo);
        if (novo.estoqueDisponivel()) {
            throw new ConflitoException("Este livro já tem exemplares " + tipo.getDescricao() + ". Aproveite!");
        }

        return repository.findFirstByUsuarioIdAndLivroIdAndTipoAndStatus(usuario.getId(), livroId, tipo, StatusAviso.PENDENTE)
                .map(AvisoDisponibilidadeDTO::new)
                .orElseGet(() -> {
                    AvisoDisponibilidade salvo = repository.save(novo);
                    logService.registrarLog(usuario.getId(), usuario.getEmail(), "AVISO_DISPONIBILIDADE_CRIADO",
                            "Pediu aviso quando o livro " + livro.getId() + " tiver exemplares " + tipo.getDescricao());
                    return new AvisoDisponibilidadeDTO(salvo);
                });
    }

    @Transactional
    public void cancelar(UUID avisoId) {
        Usuario usuario = usuarioLogado();
        AvisoDisponibilidade aviso = repository.findById(avisoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aviso não encontrado"));
        if (!aviso.getUsuario().getId().equals(usuario.getId())) {
            logService.registrarLog(usuario.getId(), usuario.getEmail(), "ACESSO_NEGADO",
                    "Tentativa de cancelar o aviso " + avisoId + " de outro usuário");
            throw new RegraDeNegocioException("Você só pode cancelar os seus próprios avisos");
        }
        if (aviso.getStatus() != StatusAviso.PENDENTE) {
            return;
        }
        aviso.setStatus(StatusAviso.CANCELADO);
        repository.save(aviso);
        logService.registrarLog(usuario.getId(), usuario.getEmail(), "AVISO_DISPONIBILIDADE_CANCELADO",
                "Cancelou o aviso do livro " + aviso.getLivro().getId() + " " + aviso.getTipo().getDescricao());
    }

    @Transactional
    public void cancelarTodosDoUsuario(UUID usuarioId) {
        repository.findByUsuarioIdAndStatusOrderByDataCriacaoDesc(usuarioId, StatusAviso.PENDENTE)
                .forEach(aviso -> aviso.setStatus(StatusAviso.CANCELADO));
    }

    @Transactional(readOnly = true)
    public List<UUID> avisosProntosParaEnvio() {
        return repository.prontosParaEnvio(StatusAviso.PENDENTE, TipoEstoque.RESERVA, TipoEstoque.COMPRA);
    }

    @Transactional
    public void enviar(UUID avisoId) {
        AvisoDisponibilidade aviso = repository.findById(avisoId).orElse(null);
        if (aviso == null || aviso.getStatus() != StatusAviso.PENDENTE || !aviso.estoqueDisponivel()) {
            return;
        }
        Usuario usuario = aviso.getUsuario();
        Livro livro = aviso.getLivro();
        if (!usuario.isAtivo()) {
            aviso.setStatus(StatusAviso.CANCELADO);
            return;
        }
        try {
            emailService.enviarAvisoDisponibilidade(usuario.getEmail(), usuario.getNome(), livro, aviso.getTipo());
            aviso.setStatus(StatusAviso.ENVIADO);
            aviso.setDataEnvio(LocalDateTime.now());
            logService.registrarLog(usuario.getId(), usuario.getEmail(), "AVISO_DISPONIBILIDADE_ENVIADO",
                    "Avisado por e-mail que o livro " + livro.getId() + " tem exemplares " + aviso.getTipo().getDescricao());
        } catch (RuntimeException e) {
            int tentativas = aviso.getTentativas() + 1;
            aviso.setTentativas(tentativas);
            if (tentativas >= AvisoDisponibilidade.TENTATIVAS_MAXIMAS) {
                aviso.setStatus(StatusAviso.FALHOU);
            }
            logger.error("Não foi possível enviar o aviso {} (tentativa {}): {}", avisoId, tentativas, e.getMessage());
        }
    }
}
