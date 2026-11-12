package br.com.pfc.biblioteca.integracao;

import br.com.pfc.biblioteca.entity.jpa.Livro;
import br.com.pfc.biblioteca.entity.jpa.Pedido;
import br.com.pfc.biblioteca.entity.jpa.Usuario;
import br.com.pfc.biblioteca.enums.Materia;
import br.com.pfc.biblioteca.enums.TipoUsuario;
import br.com.pfc.biblioteca.infra.security.JwtService;
import br.com.pfc.biblioteca.repository.LivroRepository;
import br.com.pfc.biblioteca.repository.PedidoRepository;
import br.com.pfc.biblioteca.repository.ReservaRepository;
import br.com.pfc.biblioteca.repository.UsuarioRepository;
import br.com.pfc.biblioteca.service.EmailService;
import br.com.pfc.biblioteca.service.LogAuditoriaService;
import br.com.pfc.biblioteca.service.MercadoPagoService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class TesteIntegracaoBase {

    protected static final String SENHA = "Senha@123";
    protected static final String SEGREDO_WEBHOOK = "teste";
    private static final AtomicLong SEQUENCIA = new AtomicLong();

    @Value("${local.server.port}")
    protected int porta;

    @MockitoBean
    protected EmailService emailService;

    @MockitoBean
    protected LogAuditoriaService logService;

    @MockitoSpyBean
    protected MercadoPagoService mercadoPagoService;

    @Autowired
    protected UsuarioRepository usuarioRepository;

    @Autowired
    protected LivroRepository livroRepository;

    @Autowired
    protected PedidoRepository pedidoRepository;

    @Autowired
    protected ReservaRepository reservaRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected JwtService jwtService;

    protected final ObjectMapper json = new ObjectMapper();
    protected final HttpClient http = HttpClient.newHttpClient();
    protected final Map<String, MercadoPagoService.OrderMP> ordensMercadoPago = new ConcurrentHashMap<>();

    protected record Resposta(int status, JsonNode corpo) {
        String mensagem() {
            return corpo == null ? null : corpo.path("mensagem").asText(null);
        }
    }

    @BeforeEach
    void simularMercadoPago() {
        ordensMercadoPago.clear();
        doAnswer(invocacao -> {
            Pedido pedido = invocacao.getArgument(0);
            String id = "ORDTEST" + SEQUENCIA.incrementAndGet() + "ABC";
            ordensMercadoPago.put(id, new MercadoPagoService.OrderMP(id, String.valueOf(pedido.getId()), "created", "created"));
            return new MercadoPagoService.OrderCriada(id, "https://www.mercadopago.com.br/checkout/v1/redirect?order_id=" + id);
        }).when(mercadoPagoService).criarOrder(any(Pedido.class), anyInt());
        doAnswer(invocacao -> ordensMercadoPago.get((String) invocacao.getArgument(0)))
                .when(mercadoPagoService).buscarOrder(anyString());
    }

    protected Resposta get(String caminho, String token) {
        return requisicao("GET", caminho, token, null, Map.of());
    }

    protected Resposta post(String caminho, String token, Object corpo) {
        return requisicao("POST", caminho, token, corpo, Map.of());
    }

    protected Resposta put(String caminho, String token, Object corpo) {
        return requisicao("PUT", caminho, token, corpo, Map.of());
    }

    protected Resposta delete(String caminho, String token) {
        return requisicao("DELETE", caminho, token, null, Map.of());
    }

    protected Resposta requisicao(String metodo, String caminho, String token, Object corpo, Map<String, String> cabecalhos) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + caminho))
                    .header("Content-Type", "application/json")
                    .method(metodo, corpo == null
                            ? HttpRequest.BodyPublishers.noBody()
                            : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(corpo)));
            if (token != null) {
                builder.header("Authorization", "Bearer " + token);
            }
            cabecalhos.forEach(builder::header);
            HttpResponse<String> resposta = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return new Resposta(resposta.statusCode(), lerCorpo(resposta.body()));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private JsonNode lerCorpo(String corpo) {
        if (corpo == null || corpo.isBlank()) {
            return null;
        }
        try {
            return json.readTree(corpo);
        } catch (Exception e) {
            return TextNode.valueOf(corpo);
        }
    }

    protected String emailUnico() {
        return "usuario" + SEQUENCIA.incrementAndGet() + "-" + UUID.randomUUID().toString().substring(0, 8) + "@teste.com";
    }

    protected Usuario criarUsuario(TipoUsuario tipo) {
        Usuario usuario = new Usuario();
        usuario.setNome(tipo == TipoUsuario.ESTUDANTE ? "Estudante " + SEQUENCIA.incrementAndGet() : "Funcionário " + SEQUENCIA.incrementAndGet());
        usuario.setEmail(emailUnico());
        usuario.setSenha(passwordEncoder.encode(SENHA));
        usuario.setTipo(tipo);
        return usuarioRepository.save(usuario);
    }

    protected Usuario criarEstudante() {
        return criarUsuario(TipoUsuario.ESTUDANTE);
    }

    protected Usuario criarFuncionario() {
        return criarUsuario(TipoUsuario.FUNCIONARIO);
    }

    protected String token(Usuario usuario) {
        return jwtService.gerarToken(usuario);
    }

    protected Livro criarLivro(double preco, int estoqueReserva, int estoqueCompra) {
        return criarLivro(Materia.TI, preco, estoqueReserva, estoqueCompra);
    }

    protected Livro criarLivro(Materia materia, double preco, int estoqueReserva, int estoqueCompra) {
        Livro livro = new Livro();
        livro.setTitulo("Livro de teste " + SEQUENCIA.incrementAndGet());
        livro.setAutores(List.of("Autor de teste"));
        livro.setAnoLancamento("2020");
        livro.setNumeroPagina(100);
        livro.setMateria(materia);
        livro.setPreco(preco);
        livro.setEstoqueReserva(estoqueReserva);
        livro.setEstoqueCompra(estoqueCompra);
        livro.setAtivo(true);
        return livroRepository.save(livro);
    }

    protected Livro recarregar(Livro livro) {
        return livroRepository.findById(livro.getId()).orElseThrow();
    }

    protected Resposta adicionarAoCarrinho(String token, Livro livro, int quantidade) {
        return post("/carrinho/itens", token, Map.of("livroId", livro.getId(), "quantidade", quantidade));
    }

    protected String orderDoPedido(long pedidoId) {
        return ordensMercadoPago.values().stream()
                .filter(order -> order.externalReference().equals(String.valueOf(pedidoId)))
                .map(MercadoPagoService.OrderMP::id)
                .findFirst()
                .orElseThrow();
    }

    protected void definirStatusNoMercadoPago(String orderId, String status) {
        MercadoPagoService.OrderMP atual = ordensMercadoPago.get(orderId);
        ordensMercadoPago.put(orderId, new MercadoPagoService.OrderMP(orderId, atual.externalReference(), status, status));
    }

    protected Resposta notificarWebhook(String dataId, String segredo) {
        try {
            String ts = String.valueOf(System.currentTimeMillis());
            String requestId = UUID.randomUUID().toString();
            String manifesto = "id:" + dataId.toLowerCase() + ";request-id:" + requestId + ";ts:" + ts + ";";
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String v1 = HexFormat.of().formatHex(mac.doFinal(manifesto.getBytes(StandardCharsets.UTF_8)));
            return requisicao("POST", "/pagamentos/webhook?data.id=" + dataId + "&type=order", null, Map.of("action", "order.updated"),
                    Map.of("x-signature", "ts=" + ts + ",v1=" + v1, "x-request-id", requestId));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
