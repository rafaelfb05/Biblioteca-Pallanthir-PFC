package br.com.pfc.biblioteca.service;

import br.com.pfc.biblioteca.dto.ReservaDTO;
import br.com.pfc.biblioteca.enums.StatusReserva;
import br.com.pfc.biblioteca.infra.exception.ConflitoException;
import br.com.pfc.biblioteca.infra.exception.RecursoNaoEncontradoException;
import br.com.pfc.biblioteca.infra.exception.RegraDeNegocioException;
import br.com.pfc.biblioteca.model.Livro;
import br.com.pfc.biblioteca.model.Reserva;
import br.com.pfc.biblioteca.model.Usuario;
import br.com.pfc.biblioteca.repository.LivroRepository;
import br.com.pfc.biblioteca.repository.ReservaRepository;
import br.com.pfc.biblioteca.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReservaService {

    @Autowired
    private ReservaRepository repository;
    @Autowired
    private LivroRepository livroRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Transactional
    public Reserva reservarLivro(Long usuarioId, Long livroId){
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));

        Livro livro = livroRepository.buscarComLock(livroId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Livro não encontrado"));

        if(livro.getEstoque() == null || livro.getEstoque() <= 0){
            throw new ConflitoException("Livro esgotado no momento!");
        }
        livro.setEstoque(livro.getEstoque() -1);
        livroRepository.save(livro);

        Reserva reserva = new Reserva(usuario, livro);
        return repository.save(reserva);
    }

    @Transactional
    public void cancelarReserva(Long reservaId){
        String emailLogado = SecurityContextHolder.getContext().getAuthentication().getName();

        Reserva reserva = repository.findById(reservaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva não encontrada!"));

        if(!reserva.getUsuario().getEmail().equals(emailLogado)){
            throw new RegraDeNegocioException("Reserva não encontrada");
        }

        if(reserva.getStatus() == StatusReserva.RESERVADO){
            Livro livro = reserva.getLivro();
            livro.setEstoque(livro.getEstoque() + 1);
            livroRepository.save(livro);
        }

        reserva.setStatus(StatusReserva.CANCELADO);
        repository.save(reserva);
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
                throw new RegraDeNegocioException("Você só pode ver suas próprias reservas");
            }
        }

        return repository.findByUsuarioId(usuarioId).stream()
                .map(ReservaDTO::new)
                .collect(Collectors.toList());
    }
}
