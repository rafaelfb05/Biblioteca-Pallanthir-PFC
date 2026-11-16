package br.com.pfc.biblioteca.service;

import br.com.pfc.biblioteca.dto.multa.MultaDTO;
import br.com.pfc.biblioteca.entity.jpa.Multa;
import br.com.pfc.biblioteca.entity.jpa.Reserva;
import br.com.pfc.biblioteca.entity.jpa.Usuario;
import br.com.pfc.biblioteca.enums.StatusMulta;
import br.com.pfc.biblioteca.infra.exception.ConflitoException;
import br.com.pfc.biblioteca.infra.exception.RecursoNaoEncontradoException;
import br.com.pfc.biblioteca.infra.exception.RegraDeNegocioException;
import br.com.pfc.biblioteca.repository.MultaRepository;
import br.com.pfc.biblioteca.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class MultaService {

    public static final int MINUTOS_PARA_PAGAR = 60;
    private static final int TAMANHO_MAXIMO_MOTIVO = 300;
    private static final Logger log = LoggerFactory.getLogger(MultaService.class);

    private final MultaRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final MercadoPagoService mercadoPagoService;
    private final LogAuditoriaService logService;

    public MultaService(MultaRepository repository, UsuarioRepository usuarioRepository,
                        MercadoPagoService mercadoPagoService, LogAuditoriaService logService) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.mercadoPagoService = mercadoPagoService;
        this.logService = logService;
    }

    private Usuario usuarioLogado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmailAndAtivoTrue(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
    }

    private static String emReais(BigDecimal valor) {
        return NumberFormat.getCurrencyInstance(Locale.of("pt", "BR")).format(valor);
    }

    @Transactional
    public Multa registrarPorDano(Reserva reserva, BigDecimal valorInformado, String observacao) {
        BigDecimal valor = valorInformado != null
                ? valorInformado
                : BigDecimal.valueOf(reserva.getLivro().getPreco());
        valor = valor.setScale(2, RoundingMode.HALF_UP);
        if (valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraDeNegocioException("Informe o valor da multa: o livro não tem preço cadastrado");
        }

        String motivo = observacao == null || observacao.isBlank() ? "Livro devolvido danificado" : observacao.trim();
        if (motivo.length() > TAMANHO_MAXIMO_MOTIVO) {
            throw new RegraDeNegocioException("A observação pode ter no máximo " + TAMANHO_MAXIMO_MOTIVO + " caracteres");
        }

        Usuario funcionario = usuarioLogado();
        Multa multa = repository.save(new Multa(reserva.getUsuario(), reserva.getLivro(), reserva, valor, motivo, funcionario.getId()));
        logService.registrarLog(funcionario.getId(), funcionario.getEmail(), "MULTA_APLICADA",
                "Multa " + multa.getId() + " de " + emReais(valor) + " para o usuário " + reserva.getUsuario().getId()
                        + " pelo livro " + reserva.getLivro().getId() + " devolvido danificado");
        return multa;
    }

    public void garantirSemMultaPendente(UUID usuarioId) {
        List<Multa> pendentes = repository.findByUsuarioIdAndStatus(usuarioId, StatusMulta.PENDENTE);
        if (pendentes.isEmpty()) {
            return;
        }
        BigDecimal total = pendentes.stream().map(Multa::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
        throw new ConflitoException("Você tem uma multa pendente de " + emReais(total)
                + ". Pague a multa em Minha conta para voltar a reservar e comprar livros.");
    }

    public boolean temMultaPendente(UUID usuarioId) {
        return repository.existsByUsuarioIdAndStatus(usuarioId, StatusMulta.PENDENTE);
    }

    @Transactional
    public List<MultaDTO> minhasMultas() {
        Usuario usuario = usuarioLogado();
        repository.findByUsuarioIdAndStatus(usuario.getId(), StatusMulta.PENDENTE).stream()
                .filter(multa -> multa.getMpOrderId() != null && !multa.getMpOrderId().isBlank())
                .forEach(multa -> sincronizar(multa.getMpOrderId()));
        return repository.findByUsuarioIdOrderByDataCriacaoDesc(usuario.getId()).stream()
                .map(MultaDTO::new)
                .toList();
    }

    private void sincronizar(String orderId) {
        try {
            MercadoPagoService.OrderMP order = mercadoPagoService.buscarOrder(orderId);
            if (order != null) {
                processarPagamento(order);
            }
        } catch (RuntimeException e) {
            log.warn("Não foi possível consultar a order {} da multa: {}", orderId, e.getMessage());
        }
    }

    @Transactional
    public MultaDTO iniciarPagamento(UUID multaId) {
        Usuario usuario = usuarioLogado();
        Multa multa = repository.buscarComLock(multaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Multa não encontrada"));
        if (!multa.getUsuario().getId().equals(usuario.getId())) {
            throw new RecursoNaoEncontradoException("Multa não encontrada");
        }
        if (multa.getStatus() != StatusMulta.PENDENTE) {
            throw new ConflitoException("Esta multa não está mais pendente");
        }

        LocalDateTime agora = LocalDateTime.now();
        boolean linkValido = multa.getLinkPagamento() != null && multa.getDataExpiracaoPagamento() != null
                && multa.getDataExpiracaoPagamento().isAfter(agora.plusMinutes(2));
        if (!linkValido) {
            MercadoPagoService.OrderCriada order = mercadoPagoService.criarOrderMulta(multa, MINUTOS_PARA_PAGAR);
            multa.setMpOrderId(order.id());
            multa.setLinkPagamento(order.checkoutUrl());
            multa.setDataExpiracaoPagamento(agora.plusMinutes(MINUTOS_PARA_PAGAR));
            repository.save(multa);
            logService.registrarLog(usuario.getId(), usuario.getEmail(), "MULTA_PAGAMENTO_INICIADO",
                    "Multa " + multa.getId() + ", order " + order.id());
        }
        return new MultaDTO(multa);
    }

    @Transactional
    public void processarPagamento(MercadoPagoService.OrderMP order) {
        if (order == null || order.externalReference() == null
                || !order.externalReference().startsWith(Multa.PREFIXO_REFERENCIA)) {
            return;
        }
        UUID multaId;
        try {
            multaId = UUID.fromString(order.externalReference().substring(Multa.PREFIXO_REFERENCIA.length()));
        } catch (IllegalArgumentException e) {
            return;
        }
        Multa multa = repository.buscarComLock(multaId).orElse(null);
        if (multa == null || multa.getStatus() != StatusMulta.PENDENTE || !"processed".equals(order.status())) {
            return;
        }
        multa.setStatus(StatusMulta.PAGA);
        multa.setDataPagamento(LocalDateTime.now());
        repository.save(multa);
        logService.registrarLog(multa.getUsuario().getId(), multa.getUsuario().getEmail(), "MULTA_PAGA",
                "Multa " + multa.getId() + " de " + emReais(multa.getValor()) + " paga, order " + order.id());
    }

    public List<MultaDTO> listarPendentes() {
        return repository.findByStatusOrderByDataCriacaoAsc(StatusMulta.PENDENTE).stream()
                .map(MultaDTO::new)
                .toList();
    }

    @Transactional
    public MultaDTO dispensar(UUID multaId) {
        Usuario funcionario = usuarioLogado();
        Multa multa = repository.buscarComLock(multaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Multa não encontrada"));
        if (multa.getStatus() != StatusMulta.PENDENTE) {
            throw new ConflitoException("Esta multa não está mais pendente");
        }
        multa.setStatus(StatusMulta.DISPENSADA);
        repository.save(multa);
        logService.registrarLog(funcionario.getId(), funcionario.getEmail(), "MULTA_DISPENSADA",
                "Multa " + multa.getId() + " de " + emReais(multa.getValor()) + " do usuário " + multa.getUsuario().getId() + " dispensada");
        return new MultaDTO(multa);
    }
}
