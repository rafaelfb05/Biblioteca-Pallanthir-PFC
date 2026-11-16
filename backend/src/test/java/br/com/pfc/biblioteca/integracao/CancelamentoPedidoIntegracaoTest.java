package br.com.pfc.biblioteca.integracao;

import br.com.pfc.biblioteca.entity.jpa.Livro;
import br.com.pfc.biblioteca.entity.jpa.Usuario;
import br.com.pfc.biblioteca.enums.StatusPedido;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class CancelamentoPedidoIntegracaoTest extends TesteIntegracaoBase {

    private record Compra(Usuario estudante, Livro livro, UUID pedidoId, String orderId) {
    }

    @BeforeEach
    void simularCancelamentoNoMercadoPago() {
        doNothing().when(mercadoPagoService).cancelarOrder(anyString());
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

    @Test
    void estudanteCancelaPedidoAguardandoPagamentoEOEstoqueVolta() {
        Compra compra = comprar(5, 2);
        assertThat(recarregar(compra.livro()).getEstoqueCompra()).isEqualTo(3);

        Resposta resposta = post("/pedidos/" + compra.pedidoId() + "/cancelar", token(compra.estudante()), null);

        assertThat(resposta.status()).isEqualTo(200);
        assertThat(resposta.corpo().path("status").asText()).isEqualTo("CANCELADO");
        assertThat(status(compra)).isEqualTo(StatusPedido.CANCELADO);
        assertThat(recarregar(compra.livro()).getEstoqueCompra()).isEqualTo(5);
        verify(mercadoPagoService).cancelarOrder(eq(compra.orderId()));
        assertThat(get("/carrinho", token(compra.estudante())).corpo().path("itens").size()).isEqualTo(1);
        assertThat(post("/pedidos/checkout", token(compra.estudante()), null).status()).isEqualTo(200);
    }

    @Test
    void pedidoJaPagoNaoPodeSerCancelado() {
        Compra compra = comprar(5, 1);
        definirStatusNoMercadoPago(compra.orderId(), "processed");

        Resposta resposta = post("/pedidos/" + compra.pedidoId() + "/cancelar", token(compra.estudante()), null);

        assertThat(resposta.status()).isEqualTo(409);
        assertThat(resposta.mensagem()).contains("solicite o reembolso");
        verify(mercadoPagoService, never()).cancelarOrder(anyString());
    }

    @Test
    void estudanteNaoCancelaPedidoDeOutraPessoaNemPedidoJaCancelado() {
        Compra compra = comprar(5, 1);

        assertThat(post("/pedidos/" + compra.pedidoId() + "/cancelar", token(criarEstudante()), null).status()).isEqualTo(400);
        assertThat(post("/pedidos/" + compra.pedidoId() + "/cancelar", token(compra.estudante()), null).status()).isEqualTo(200);
        assertThat(post("/pedidos/" + compra.pedidoId() + "/cancelar", token(compra.estudante()), null).status()).isEqualTo(409);
    }

    @Test
    void pagamentoQueChegaDepoisDoCancelamentoNaoReativaOPedido() {
        Compra compra = comprar(5, 1);
        post("/pedidos/" + compra.pedidoId() + "/cancelar", token(compra.estudante()), null);
        definirStatusNoMercadoPago(compra.orderId(), "processed");

        notificarWebhook(compra.orderId(), SEGREDO_WEBHOOK);

        assertThat(status(compra)).isEqualTo(StatusPedido.CANCELADO);
        verify(logService).registrarLog(any(UUID.class), anyString(), eq("PAGAMENTO_EM_PEDIDO_EXPIRADO"), anyString());
    }
}
