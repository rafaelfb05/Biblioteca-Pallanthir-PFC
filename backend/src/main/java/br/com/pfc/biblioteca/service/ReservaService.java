package br.com.pfc.biblioteca.service;

import br.com.pfc.biblioteca.dto.ReservaDTO;
import br.com.pfc.biblioteca.enums.StatusReserva;
import br.com.pfc.biblioteca.infra.exception.ConflitoException;
import br.com.pfc.biblioteca.infra.exception.RecursoNaoEncontradoException;
import br.com.pfc.biblioteca.infra.exception.RegraDeNegocioException;
import br.com.pfc.biblioteca.entity.jpa.Livro;
import br.com.pfc.biblioteca.entity.jpa.Reserva;
import br.com.pfc.biblioteca.entity.jpa.Usuario;
import br.com.pfc.biblioteca.repository.LivroRepository;
import br.com.pfc.biblioteca.repository.ReservaRepository;
import br.com.pfc.biblioteca.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReservaService {

    private final ReservaRepository repository;
    private final LivroRepository livroRepository;
    private final UsuarioRepository usuarioRepository;
    private final LogAuditoriaService logService;

    public ReservaService(ReservaRepository repository, LivroRepository livroRepository, UsuarioRepository usuarioRepository, LogAuditoriaService logService) {
        this.repository = repository;
        this.livroRepository = livroRepository;
        this.usuarioRepository = usuarioRepository;
        this.logService = logService;
    }

    @Transactional
    public Reserva reservarLivro(Long usuarioId, Long livroId){
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));

        Livro livro = livroRepository.buscarComLock(livroId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Livro não encontrado"));

        if(livro.getEstoque() == null || livro.getEstoque() <= 0){
            logService.registrarLog(usuario.getId(), usuario.getEmail(), "FALHA_RESERVA", "Livro " + livro.getId() + " esgotado");
            throw new ConflitoException("Livro esgotado no momento!");
        }
        livro.setEstoque(livro.getEstoque() -1);
        livroRepository.save(livro);

        Reserva reserva = repository.save(new Reserva(usuario, livro));
        logService.registrarLog(usuario.getId(), usuario.getEmail(), "RESERVA_LIVRO", "Reserva " + reserva.getId() + " do livro " + livro.getId() + " criada");
        return reserva;
    }

    @Transactional
    public void cancelarReserva(Long reservaId){
        String emailLogado = SecurityContextHolder.getContext().getAuthentication().getName();

        Reserva reserva = repository.findById(reservaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva não encontrada!"));

        if(!reserva.getUsuario().getEmail().equals(emailLogado)){
            logService.registrarLogUsuarioLogado("ACESSO_NEGADO", "Tentativa de cancelar a reserva " + reservaId + " de outro usuário");
            throw new RegraDeNegocioException("Reserva não encontrada");
        }

        if(reserva.getStatus() == StatusReserva.RESERVADO){
            Livro livro = reserva.getLivro();
            livro.setEstoque(livro.getEstoque() + 1);
            livroRepository.save(livro);
        }

        reserva.setStatus(StatusReserva.CANCELADO);
        repository.save(reserva);
        logService.registrarLog(reserva.getUsuario().getId(), emailLogado, "CANCELAMENTO_RESERVA", "Reserva " + reservaId + " cancelada");
    }

    @Transactional
    public void devolverLivro(Long reservaId){
        Reserva reserva = repository.findById(reservaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva não encontrada!"));

        Livro livro = reserva.getLivro();
        livro.setEstoque(livro.getEstoque() + 1);
        livroRepository.save(livro);

        reserva.setStatus(StatusReserva.DEVOLVIDO);
        repository.save(reserva);
        logService.registrarLogUsuarioLogado("DEVOLUCAO_LIVRO", "Reserva " + reservaId + " do usuário " + reserva.getUsuario().getId() + " devolvida");
    }

    public List<ReservaDTO> listarReservasPorUsuario(Long usuarioId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String emailLogado = auth.getName();
        boolean ehFuncionario = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_FUNCIONARIO"));

        if(!ehFuncionario) {
            Usuario usuarioLogado = usuarioRepository.findByEmailAndAtivoTrue(emailLogado)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));

            if(!usuarioLogado.getId().equals(usuarioId)){
                logService.registrarLog(usuarioLogado.getId(), usuarioLogado.getEmail(), "ACESSO_NEGADO", "Tentativa de listar as reservas do usuário " + usuarioId);
                throw new RegraDeNegocioException("Você só pode ver suas próprias reservas");
            }
        }

        return repository.findByUsuarioId(usuarioId).stream()
                .map(ReservaDTO::new)
                .collect(Collectors.toList());
    }
}
