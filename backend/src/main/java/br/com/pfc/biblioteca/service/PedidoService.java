package br.com.pfc.biblioteca.service;

import br.com.pfc.biblioteca.dto.pedido.PedidoDTO;
import br.com.pfc.biblioteca.entity.jpa.*;
import br.com.pfc.biblioteca.enums.StatusPedido;
import br.com.pfc.biblioteca.infra.exception.ConflitoException;
import br.com.pfc.biblioteca.infra.exception.RecursoNaoEncontradoException;
import br.com.pfc.biblioteca.infra.exception.RegraDeNegocioException;
import br.com.pfc.biblioteca.repository.ItemCarrinhoRepository;
import br.com.pfc.biblioteca.repository.LivroRepository;
import br.com.pfc.biblioteca.repository.PedidoRepository;
import br.com.pfc.biblioteca.repository.UsuarioRepository;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;

import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class PedidoService {

    private static final Logger log = LoggerFactory.getLogger(PedidoService.class);
    private static final int MINUTOS_VALIDADE = 30;

    private final PedidoRepository repository;
    private final ItemCarrinhoRepository itemCarrinhoRepository;
    private final LivroRepository livroRepository;
    private final UsuarioRepository usuarioRepository;
    private final MercadoPagoService mercadoPagoService;
    private final LogAuditoriaService logService;

    public PedidoService(PedidoRepository repository, ItemCarrinhoRepository itemCarrinhoRepository, LivroRepository livroRepository, UsuarioRepository usuarioRepository, MercadoPagoService mercadoPagoService, LogAuditoriaService logService) {
        this.repository = repository;
        this.itemCarrinhoRepository = itemCarrinhoRepository;
        this.livroRepository = livroRepository;
        this.usuarioRepository = usuarioRepository;
        this.mercadoPagoService = mercadoPagoService;
        this.logService = logService;
    }

    @Transactional
    public PedidoDTO finalizarCompra(){
        Usuario usuario = usuarioLogado();

        List<ItemCarrinho> itensCarrinho = new ArrayList<>(itemCarrinhoRepository.findByUsuarioIdAndAtivoTrue(usuario.getId()));
        if(itensCarrinho.isEmpty()){
            throw new RegraDeNegocioException("O carrinho está vazio");
        }

        itensCarrinho.sort(Comparator.comparing(ic -> ic.getLivro().getId()));
        Pedido pedido = new Pedido(usuario);
        BigDecimal total = BigDecimal.ZERO;

        for(ItemCarrinho ic : itensCarrinho) {
            Livro livro = livroRepository.buscarComLock(ic.getLivro().getId())
                    .filter(Livro::isAtivo)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Livro não encontrado"));
            int estoque = livro.getEstoqueCompra() == null ? 0 : livro.getEstoqueCompra();

            if (estoque < ic.getQuantidade()) {
                throw new ConflitoException("O estoque de " + livro.getTitulo() + " está indisponível");
            }

            if (livro.getPreco() <= 0) {
                throw new RegraDeNegocioException("Livro sem preço definido: " + livro.getTitulo());
            }

            BigDecimal preco = BigDecimal.valueOf(livro.getPreco()).setScale(2, RoundingMode.HALF_UP);
            livro.setEstoqueCompra(estoque - ic.getQuantidade());
            livroRepository.save(livro);

            pedido.adicionarItem(new ItemPedido(pedido, livro, ic.getQuantidade(), preco));
            total = total.add(preco.multiply(BigDecimal.valueOf(ic.getQuantidade())));
            ic.setAtivo(false);
        }

            pedido.setTotal(total);
            pedido.setDataExpiracao(LocalDateTime.now().plusMinutes(MINUTOS_VALIDADE));
            repository.save(pedido);

            MercadoPagoService.OrderCriada order = mercadoPagoService.criarOrder(pedido, MINUTOS_VALIDADE);
            pedido.setMpOrderId(order.id());
            pedido.setLinkPagamento(order.checkoutUrl);
            repository.save(pedido);
            itemCarrinhoRepository.saveAll(itensCarrinho);

            logService.registrarLog(usuario.getId(), usuario.getEmail(), "PEDIDO_CRIADO",
                    "Pedido: " + pedido.getId() + ", total: " + total);
            return new PedidoDTO(pedido);
    }

    @Transactional(readOnly = true)
    public List<PedidoDTO> meusPedidos(){
        return repository.findByUsuarioIdOrderByDataCriacaoDesc(usuarioLogado().getId())
                .stream().map(PedidoDTO::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PedidoDTO obterPedido(Long id){
        Usuario usuario = usuarioLogado();
        Pedido pedido = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado"));

        if(!pedido.getUsuario().getId().equals(usuario.getId())){
            throw new RegraDeNegocioException("Você só pode ver os seus próprios pedidos");
        }
        return new PedidoDTO(pedido);
    }

    @Transactional
    public void processarNotificacao(String orderId){
        MercadoPagoService.OrderMP order = mercadoPagoService.buscarOrder(orderId);
        if (order == null || order.externalReference() == null
                || !order.externalReference().matches("\\d+")) return;

        Pedido pedido = repository.buscarComLock(Long.valueOf(order.externalReference())).orElse(null);
        if (pedido == null) return;

        boolean pago = "processed".equals(order.status());

        if (pedido.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO) {
            if (pago && pedido.getStatus() == StatusPedido.EXPIRADO) {
                logService.registrarLog(pedido.getUsuario().getId(), pedido.getUsuario().getEmail(),
                        "PAGAMENTO_EM_PEDIDO_EXPIRADO", "Pedido " + pedido.getId() + ", order " + orderId);
            }
            return;
        }

        if (pago) {
            pedido.setStatus(StatusPedido.APROVADO);
            repository.save(pedido);
            logService.registrarLog(pedido.getUsuario().getId(), pedido.getUsuario().getEmail(),
                    "PAGAMENTO_APROVADO", "Pedido " + pedido.getId() + ", order " + orderId);
        } else if ("canceled".equals(order.status())) {
            expirar(pedido);
        }
    }

    @Transactional
    public void expirarPedidosVencidos() {
        LocalDateTime limite = LocalDateTime.now().minusMinutes(2); // folga pra pagamento em trânsito

        for (Pedido candidato : repository
                .findByStatusAndDataExpiracaoBefore(StatusPedido.AGUARDANDO_PAGAMENTO, limite)) {
            Pedido pedido = repository.buscarComLock(candidato.getId()).orElse(null);
            if (pedido != null && pedido.getStatus() == StatusPedido.AGUARDANDO_PAGAMENTO) {
                expirar(pedido);
            }
        }
    }

    private void expirar(Pedido pedido) {
        for (ItemPedido item : pedido.getItens()) {
            Livro livro = livroRepository.buscarComLock(item.getLivro().getId()).orElse(null);
            if (livro == null) continue;
            int atual = livro.getEstoqueCompra() == null ? 0 : livro.getEstoqueCompra();
            livro.setEstoqueCompra(atual + item.getQuantidade());
            livroRepository.save(livro);
        }
        pedido.setStatus(StatusPedido.EXPIRADO);
        repository.save(pedido);
        logService.registrarLog(pedido.getUsuario().getId(), pedido.getUsuario().getEmail(),
                "PEDIDO_EXPIRADO", "Pedido " + pedido.getId());
    }

    private Usuario usuarioLogado(){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmailAndAtivoTrue(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
    }
}
