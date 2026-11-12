package br.com.pfc.biblioteca.integracao;

import br.com.pfc.biblioteca.entity.jpa.Livro;
import br.com.pfc.biblioteca.entity.jpa.Pedido;
import br.com.pfc.biblioteca.entity.jpa.Usuario;
import br.com.pfc.biblioteca.enums.StatusPedido;
import br.com.pfc.biblioteca.service.PedidoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

class PagamentoIntegracaoTest extends TesteIntegracaoBase {

    @Autowired
    private PedidoService pedidoService;

    private record Compra(Usuario estudante, Livro livro, UUID pedidoId, String orderId) {
    }

    private Compra comprar(int estoqueCompra, int quantidade) {
        Usuario estudante = criarEstudante();
        Livro livro = criarLivro(25, 0, estoqueCompra);
        adicionarAoCarrinho(token(estudante), livro, quantidade);
        String pedidoId = post("/pedidos/checkout", token(estudante), null).corpo().path("id").asText();
        return new Compra(estudante, livro, UUID.fromString(pedidoId), orderDoPedido(pedidoId));
    }

    private StatusPedido status(Compra compra) {
        return pedidoRepository.findById(compra.pedidoId()).orElseThrow().getStatus();
    }

    private int itensNoCarrinho(Compra compra) {
        return get("/carrinho", token(compra.estudante())).corpo().path("itens").size();
    }

    private void vencerPrazo(Compra compra, int minutosAtras) {
        Pedido pedido = pedidoRepository.findById(compra.pedidoId()).orElseThrow();
        pedido.setDataExpiracao(LocalDateTime.now().minusMinutes(minutosAtras));
        pedidoRepository.save(pedido);
    }

    @Test
    void webhookSemAssinaturaOuComAssinaturaErradaERecusado() {
        Compra compra = comprar(5, 1);
        definirStatusNoMercadoPago(compra.orderId(), "processed");

        Resposta semAssinatura = requisicao("POST", "/pagamentos/webhook?data.id=" + compra.orderId(), null, Map.of(), Map.of());
        Resposta assinaturaErrada = notificarWebhook(compra.orderId(), "segredo-errado");

        assertThat(semAssinatura.status()).isEqualTo(401);
        assertThat(assinaturaErrada.status()).isEqualTo(401);
        assertThat(status(compra)).isEqualTo(StatusPedido.AGUARDANDO_PAGAMENTO);
    }

    @Test
    void pagamentoProcessadoAprovaOPedidoETiraOsItensDoCarrinho() {
        Compra compra = comprar(5, 2);
        definirStatusNoMercadoPago(compra.orderId(), "processed");

        Resposta resposta = notificarWebhook(compra.orderId(), SEGREDO_WEBHOOK);

        assertThat(resposta.status()).isEqualTo(200);
        assertThat(status(compra)).isEqualTo(StatusPedido.APROVADO);
        assertThat(itensNoCarrinho(compra)).isZero();
        assertThat(recarregar(compra.livro()).getEstoqueCompra()).isEqualTo(3);
        verify(logService).registrarLog(any(UUID.class), anyString(), eq("PAGAMENTO_APROVADO"), anyString());
    }

    @Test
    void notificacaoRepetidaNaoAlteraPedidoAprovado() {
        Compra compra = comprar(5, 2);
        definirStatusNoMercadoPago(compra.orderId(), "processed");
        notificarWebhook(compra.orderId(), SEGREDO_WEBHOOK);

        assertThat(notificarWebhook(compra.orderId(), SEGREDO_WEBHOOK).status()).isEqualTo(200);
        assertThat(status(compra)).isEqualTo(StatusPedido.APROVADO);
        assertThat(recarregar(compra.livro()).getEstoqueCompra()).isEqualTo(3);
    }

    @Test
    void notificacaoDePagamentoAindaPendenteNaoMudaNada() {
        Compra compra = comprar(5, 1);

        assertThat(notificarWebhook(compra.orderId(), SEGREDO_WEBHOOK).status()).isEqualTo(200);
        assertThat(status(compra)).isEqualTo(StatusPedido.AGUARDANDO_PAGAMENTO);
        assertThat(itensNoCarrinho(compra)).isEqualTo(1);
    }

    @Test
    void pagamentoCanceladoExpiraOPedidoEDevolveOEstoque() {
        Compra compra = comprar(5, 2);
        definirStatusNoMercadoPago(compra.orderId(), "canceled");

        notificarWebhook(compra.orderId(), SEGREDO_WEBHOOK);

        assertThat(status(compra)).isEqualTo(StatusPedido.EXPIRADO);
        assertThat(recarregar(compra.livro()).getEstoqueCompra()).isEqualTo(5);
        assertThat(itensNoCarrinho(compra)).isZero();
    }

    @Test
    void pedidoVencidoExpiraAutomaticamente() {
        Compra compra = comprar(5, 2);
        vencerPrazo(compra, 5);

        pedidoService.expirarPedidosVencidos();

        assertThat(status(compra)).isEqualTo(StatusPedido.EXPIRADO);
        assertThat(recarregar(compra.livro()).getEstoqueCompra()).isEqualTo(5);
        assertThat(itensNoCarrinho(compra)).isZero();
    }

    @Test
    void pedidoDentroDaToleranciaNaoExpira() {
        Compra compra = comprar(5, 1);
        vencerPrazo(compra, 1);

        pedidoService.expirarPedidosVencidos();

        assertThat(status(compra)).isEqualTo(StatusPedido.AGUARDANDO_PAGAMENTO);
    }

    @Test
    void pagamentoAprovadoDepoisDaExpiracaoNaoReativaOPedido() {
        Compra compra = comprar(5, 1);
        vencerPrazo(compra, 5);
        pedidoService.expirarPedidosVencidos();
        definirStatusNoMercadoPago(compra.orderId(), "processed");

        notificarWebhook(compra.orderId(), SEGREDO_WEBHOOK);

        assertThat(status(compra)).isEqualTo(StatusPedido.EXPIRADO);
        assertThat(recarregar(compra.livro()).getEstoqueCompra()).isEqualTo(5);
        verify(logService).registrarLog(any(UUID.class), anyString(), eq("PAGAMENTO_EM_PEDIDO_EXPIRADO"), anyString());
    }

    @Test
    void listarPedidosConsultaOMercadoPagoMesmoSemWebhook() {
        Compra compra = comprar(5, 1);
        definirStatusNoMercadoPago(compra.orderId(), "processed");

        Resposta resposta = get("/pedidos", token(compra.estudante()));

        assertThat(resposta.corpo().get(0).path("status").asText()).isEqualTo("APROVADO");
        assertThat(itensNoCarrinho(compra)).isZero();
    }

    @Test
    void pedidoExpiradoLiberaNovoCheckout() {
        Compra compra = comprar(5, 1);
        vencerPrazo(compra, 5);
        pedidoService.expirarPedidosVencidos();
        adicionarAoCarrinho(token(compra.estudante()), compra.livro(), 1);

        assertThat(post("/pedidos/checkout", token(compra.estudante()), null).status()).isEqualTo(200);
    }
}
