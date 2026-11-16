package br.com.pfc.biblioteca.service;

import java.util.UUID;
import br.com.pfc.biblioteca.dto.reserva.DevolucaoRequest;
import br.com.pfc.biblioteca.dto.reserva.ReservaDTO;
import br.com.pfc.biblioteca.dto.reserva.ReservaGestaoDTO;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReservaService {

    private final ReservaRepository repository;
    private final LivroRepository livroRepository;
    private final UsuarioRepository usuarioRepository;
    private final LogAuditoriaService logService;
    private final MultaService multaService;

    public static final int RESERVAS_EM_ABERTO_POR_ESTUDANTE = 3;
    private static final List<StatusReserva> EM_ABERTO = List.of(StatusReserva.RESERVADO, StatusReserva.ENTREGUE);

    private DocumentoService documentoService;

    @org.springframework.beans.factory.annotation.Autowired
    public void setDocumentoService(DocumentoService documentoService) {
        this.documentoService = documentoService;
    }

    public ReservaService(ReservaRepository repository, LivroRepository livroRepository, UsuarioRepository usuarioRepository, LogAuditoriaService logService, MultaService multaService) {
        this.multaService = multaService;
        this.repository = repository;
        this.livroRepository = livroRepository;
        this.usuarioRepository = usuarioRepository;
        this.logService = logService;
    }

    @Transactional
    public Reserva reservarLivro(UUID usuarioId, UUID livroId, String politicaVersao){
        String versaoDaPolitica = politicaVersao == null || politicaVersao.isBlank() ? null : politicaVersao.trim();
        if (versaoDaPolitica != null && !versaoDaPolitica.matches("^[A-Za-z0-9_-]{1,64}$")) {
            throw new RegraDeNegocioException("Versão da política de reservas inválida");
        }
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));

        String emailLogado = SecurityContextHolder.getContext().getAuthentication().getName();
        if(!usuario.getEmail().equals(emailLogado)){
            logService.registrarLogUsuarioLogado("ACESSO_NEGADO", "Tentativa de reservar em nome do usuário " + usuarioId);
            throw new RegraDeNegocioException("Você só pode reservar livros para a sua própria conta");
        }
        multaService.garantirSemMultaPendente(usuario.getId());
        garantirSemDevolucaoAtrasada(usuario);
        documentoService.garantirTermosAceitos(usuario);
        if (versaoDaPolitica == null || !versaoDaPolitica.equals(documentoService.versaoVigenteDaPoliticaDeReservas())) {
            logService.registrarLog(usuario.getId(), usuario.getEmail(), "FALHA_RESERVA", "Política de reservas não aceita na versão vigente");
            throw new RegraDeNegocioException("Leia e aceite a versão atual da Política de Uso de Reservas para reservar");
        }

        Livro livro = livroRepository.buscarComLock(livroId)
                .filter(Livro::isAtivo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Livro não encontrado"));

        if (repository.existsByUsuarioIdAndLivroIdAndStatusIn(usuario.getId(), livro.getId(), EM_ABERTO)) {
            throw new ConflitoException("Você já tem uma reserva em aberto deste livro");
        }
        if (repository.countByUsuarioIdAndStatusIn(usuario.getId(), EM_ABERTO) >= RESERVAS_EM_ABERTO_POR_ESTUDANTE) {
            throw new ConflitoException("Você já tem " + RESERVAS_EM_ABERTO_POR_ESTUDANTE
                    + " reservas em aberto. Devolva ou cancele uma delas para reservar outro livro.");
        }

        if(livro.getEstoqueReserva() == null || livro.getEstoqueReserva() <= 0){
            logService.registrarLog(usuario.getId(), usuario.getEmail(), "FALHA_RESERVA", "Livro " + livro.getId() + " esgotado");
            throw new ConflitoException("Livro esgotado no momento!");
        }
        livro.setEstoqueReserva(livro.getEstoqueReserva() -1);
        livroRepository.save(livro);

        Reserva nova = new Reserva(usuario, livro);
        if (versaoDaPolitica != null) {
            nova.setPoliticaVersao(versaoDaPolitica);
            nova.setDataAceitePolitica(LocalDateTime.now());
        }
        Reserva reserva = repository.save(nova);
        logService.registrarLog(usuario.getId(), usuario.getEmail(), "RESERVA_LIVRO", "Reserva " + reserva.getId() + " do livro " + livro.getId() + " criada"
                + (versaoDaPolitica == null ? "" : ", política de reservas aceita (versão " + versaoDaPolitica + ")"));
        return reserva;
    }

    private void garantirSemDevolucaoAtrasada(Usuario usuario) {
        LocalDateTime agora = LocalDateTime.now();
        repository.findByUsuarioIdAndStatus(usuario.getId(), StatusReserva.ENTREGUE).stream()
                .filter(reserva -> reserva.getPrazoDevolucao() != null && reserva.getPrazoDevolucao().isBefore(agora))
                .findFirst()
                .ifPresent(atrasada -> {
                    logService.registrarLog(usuario.getId(), usuario.getEmail(), "FALHA_RESERVA",
                            "Devolução atrasada da reserva " + atrasada.getId());
                    throw new ConflitoException("Você está com a devolução de \"" + atrasada.getLivro().getTitulo()
                            + "\" atrasada. Devolva o livro na biblioteca para voltar a fazer reservas.");
                });
    }

    @Transactional
    public void cancelarReserva(UUID reservaId){
        String emailLogado = SecurityContextHolder.getContext().getAuthentication().getName();

        Reserva reserva = repository.findById(reservaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva não encontrada!"));

        if(!reserva.getUsuario().getEmail().equals(emailLogado)){
            logService.registrarLogUsuarioLogado("ACESSO_NEGADO", "Tentativa de cancelar a reserva " + reservaId + " de outro usuário");
            throw new RegraDeNegocioException("Reserva não encontrada");
        }

        if(reserva.getStatus() != StatusReserva.RESERVADO){
            throw new RegraDeNegocioException("Só é possível cancelar reservas que ainda não foram entregues");
        }

        Livro livro = reserva.getLivro();
        livro.setEstoqueReserva(livro.getEstoqueReserva() + 1);
        livroRepository.save(livro);

        reserva.setStatus(StatusReserva.CANCELADO);
        repository.save(reserva);
        logService.registrarLog(reserva.getUsuario().getId(), emailLogado, "CANCELAMENTO_RESERVA", "Reserva " + reservaId + " cancelada");
    }

    @Transactional
    public void entregarLivro(UUID reservaId){
        Reserva reserva = repository.findById(reservaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva não encontrada!"));

        if(reserva.getStatus() != StatusReserva.RESERVADO){
            throw new RegraDeNegocioException("Só é possível entregar livros de reservas em aberto");
        }

        reserva.setStatus(StatusReserva.ENTREGUE);
        reserva.setDataEntrega(LocalDateTime.now());
        repository.save(reserva);
        logService.registrarLogUsuarioLogado("ENTREGA_LIVRO", "Reserva " + reservaId + " do usuário " + reserva.getUsuario().getId() + " entregue");
    }

    @Transactional
    public void devolverLivro(UUID reservaId, DevolucaoRequest request){
        Reserva reserva = repository.findById(reservaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva não encontrada!"));

        if(reserva.getStatus() != StatusReserva.ENTREGUE){
            throw new RegraDeNegocioException("Só é possível devolver livros que já foram entregues");
        }

        boolean danificado = request != null && Boolean.TRUE.equals(request.danificado());
        if (danificado) {
            multaService.registrarPorDano(reserva, request.valorMulta(), request.observacao());
        } else {
            Livro livro = reserva.getLivro();
            livro.setEstoqueReserva(livro.getEstoqueReserva() + 1);
            livroRepository.save(livro);
        }

        reserva.setDanificado(danificado);
        reserva.setStatus(StatusReserva.DEVOLVIDO);
        repository.save(reserva);
        logService.registrarLogUsuarioLogado(danificado ? "DEVOLUCAO_COM_DANO" : "DEVOLUCAO_LIVRO",
                "Reserva " + reservaId + " do usuário " + reserva.getUsuario().getId()
                        + (danificado ? " devolvida com o livro danificado" : " devolvida"));
    }

    public List<UUID> reservasComPrazoVencido() {
        return repository.findByStatusAndDataReservaBefore(StatusReserva.RESERVADO,
                        LocalDateTime.now().minusDays(Reserva.DIAS_PARA_RETIRADA))
                .stream()
                .map(Reserva::getId)
                .toList();
    }

    @Transactional
    public void cancelarPorPrazo(UUID reservaId) {
        Reserva reserva = repository.buscarComLock(reservaId).orElse(null);
        if (reserva == null || reserva.getStatus() != StatusReserva.RESERVADO
                || reserva.getPrazoRetirada() == null || reserva.getPrazoRetirada().isAfter(LocalDateTime.now())) {
            return;
        }

        Livro livro = livroRepository.buscarComLock(reserva.getLivro().getId()).orElse(reserva.getLivro());
        int atual = livro.getEstoqueReserva() == null ? 0 : livro.getEstoqueReserva();
        livro.setEstoqueReserva(atual + 1);
        livroRepository.save(livro);

        reserva.setStatus(StatusReserva.CANCELADO);
        repository.save(reserva);
        logService.registrarLog(reserva.getUsuario().getId(), reserva.getUsuario().getEmail(), "RESERVA_EXPIRADA",
                "Reserva " + reserva.getId() + " cancelada automaticamente: o livro não foi retirado em "
                        + Reserva.DIAS_PARA_RETIRADA + " dias");
    }

    public List<ReservaGestaoDTO> listarReservasEmAberto() {
        return repository.findByStatusInOrderByDataReservaAsc(List.of(StatusReserva.RESERVADO, StatusReserva.ENTREGUE))
                .stream()
                .map(ReservaGestaoDTO::new)
                .collect(Collectors.toList());
    }

    public List<ReservaDTO> listarReservasPorUsuario(UUID usuarioId) {
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

        return repository.findByUsuarioIdOrderByDataReservaDesc(usuarioId).stream()
                .map(ReservaDTO::new)
                .collect(Collectors.toList());
    }
}
