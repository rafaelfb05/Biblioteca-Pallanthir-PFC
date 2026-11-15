package br.com.pfc.biblioteca.integracao;

import br.com.pfc.biblioteca.entity.jpa.Livro;
import br.com.pfc.biblioteca.entity.jpa.Pedido;
import br.com.pfc.biblioteca.entity.jpa.Usuario;
import br.com.pfc.biblioteca.enums.StatusPedido;
import br.com.pfc.biblioteca.infra.exception.ConflitoException;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import br.com.pfc.biblioteca.schedule.PrazosDeRetiradaJob;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class RetiradaReembolsoIntegracaoTest extends TesteIntegracaoBase {

    @Autowired
    private PrazosDeRetiradaJob prazosDeRetiradaJob;

    private record Compra(Usuario estudante, Livro livro, UUID pedidoId, String orderId) {
    }

    @BeforeEach
    void simularReembolsoNoMercadoPago() {
        doNothing().when(mercadoPagoService).reembolsarOrder(anyString());
    }

    private Compra compraAprovada(int estoqueCompra, int quantidade) {
        Usuario estudante = criarEstudante();
        Livro livro = criarLivro(25, 0, estoqueCompra);
        adicionarAoCarrinho(token(estudante), livro, quantidade);
        String pedidoId = post("/pedidos/checkout", token(estudante), null).corpo().path("id").asText();
        String orderId = orderDoPedido(pedidoId);
        definirStatusNoMercadoPago(orderId, "processed");
        notificarWebhook(orderId, SEGREDO_WEBHOOK);
        return new Compra(estudante, livro, UUID.fromString(pedidoId), orderId);
    }

    private StatusPedido status(Compra compra) {
        return pedidoRepository.findById(compra.pedidoId()).orElseThrow().getStatus();
    }

    private boolean pendenteDeRetirada(String tokenFuncionario, Compra compra) {
        for (JsonNode pedido : get("/pedidos/pendentes-retirada", tokenFuncionario).corpo()) {
            if (pedido.path("id").asText().equals(compra.pedidoId().toString())) {
                return true;
            }
        }
        return false;
    }

    @Test
    void funcionarioVePedidoPagoERegistraARetirada() {
        Compra compra = compraAprovada(5, 2);
        String funcionario = token(criarFuncionario());

        assertThat(pendenteDeRetirada(funcionario, compra)).isTrue();

        Resposta entrega = put("/pedidos/" + compra.pedidoId() + "/entregar", funcionario, null);

        assertThat(entrega.status()).isEqualTo(200);
        assertThat(entrega.corpo().path("status").asText()).isEqualTo("ENTREGUE");
        assertThat(entrega.corpo().path("dataEntrega").asText()).isNotBlank();
        assertThat(status(compra)).isEqualTo(StatusPedido.ENTREGUE);
        assertThat(pendenteDeRetirada(funcionario, compra)).isFalse();
        assertThat(put("/pedidos/" + compra.pedidoId() + "/entregar", funcionario, null).status()).isEqualTo(409);
    }

    @Test
    void entregaExigePagamentoAprovadoEFuncionario() {
        Usuario estudante = criarEstudante();
        Livro livro = criarLivro(25, 0, 3);
        adicionarAoCarrinho(token(estudante), livro, 1);
        String pendente = post("/pedidos/checkout", token(estudante), null).corpo().path("id").asText();

        assertThat(put("/pedidos/" + pendente + "/entregar", token(criarFuncionario()), null).status()).isEqualTo(409);
        assertThat(put("/pedidos/" + pendente + "/entregar", token(estudante), null).status()).isEqualTo(403);
        assertThat(get("/pedidos/pendentes-retirada", token(estudante)).status()).isEqualTo(403);
    }

    @Test
    void reembolsoDevolveOEstoqueEAvisaOMercadoPago() {
        Compra compra = compraAprovada(5, 2);
        assertThat(recarregar(compra.livro()).getEstoqueCompra()).isEqualTo(3);

        Resposta reembolso = post("/pedidos/" + compra.pedidoId() + "/reembolso", token(compra.estudante()), null);

        assertThat(reembolso.status()).isEqualTo(200);
        assertThat(reembolso.corpo().path("status").asText()).isEqualTo("REEMBOLSADO");
        assertThat(reembolso.corpo().path("dataReembolso").asText()).isNotBlank();
        assertThat(status(compra)).isEqualTo(StatusPedido.REEMBOLSADO);
        assertThat(recarregar(compra.livro()).getEstoqueCompra()).isEqualTo(5);
        verify(mercadoPagoService).reembolsarOrder(eq(compra.orderId()));
        assertThat(post("/pedidos/" + compra.pedidoId() + "/reembolso", token(compra.estudante()), null).status()).isEqualTo(409);
    }

    @Test
    void pedidoRetiradoNaoPodeSerReembolsado() {
        Compra compra = compraAprovada(5, 1);
        put("/pedidos/" + compra.pedidoId() + "/entregar", token(criarFuncionario()), null);

        Resposta reembolso = post("/pedidos/" + compra.pedidoId() + "/reembolso", token(compra.estudante()), null);

        assertThat(reembolso.status()).isEqualTo(409);
        assertThat(status(compra)).isEqualTo(StatusPedido.ENTREGUE);
        verify(mercadoPagoService, never()).reembolsarOrder(anyString());
    }

    @Test
    void estudanteNaoReembolsaPedidoDeOutraPessoa() {
        Compra compra = compraAprovada(5, 1);

        Resposta reembolso = post("/pedidos/" + compra.pedidoId() + "/reembolso", token(criarEstudante()), null);

        assertThat(reembolso.status()).isEqualTo(400);
        assertThat(status(compra)).isEqualTo(StatusPedido.APROVADO);
    }

    @Test
    void falhaNoMercadoPagoDesfazOReembolso() {
        Compra compra = compraAprovada(5, 2);
        doThrow(new ConflitoException("O Mercado Pago não permitiu o reembolso deste pedido"))
                .when(mercadoPagoService).reembolsarOrder(anyString());

        Resposta reembolso = post("/pedidos/" + compra.pedidoId() + "/reembolso", token(compra.estudante()), null);

        assertThat(reembolso.status()).isEqualTo(409);
        assertThat(status(compra)).isEqualTo(StatusPedido.APROVADO);
        assertThat(recarregar(compra.livro()).getEstoqueCompra()).isEqualTo(3);
    }

    @Test
    void contaComPedidoAguardandoRetiradaNaoPodeSerExcluida() {
        Compra compra = compraAprovada(5, 1);

        Resposta exclusao = delete("/usuarios/deletar/" + compra.estudante().getId(), token(compra.estudante()));

        assertThat(exclusao.status()).isEqualTo(409);
        assertThat(exclusao.mensagem()).contains("pedidos em andamento");
        assertThat(usuarioRepository.findById(compra.estudante().getId()).orElseThrow().isAtivo()).isTrue();
    }

    @Test
    void compraPagaNaoRetiradaEmQuinzeDiasEReembolsadaAutomaticamente() {
        Compra vencida = compraAprovada(5, 2);
        Compra noPrazo = compraAprovada(5, 1);
        Pedido pedido = pedidoRepository.findById(vencida.pedidoId()).orElseThrow();
        assertThat(pedido.getDataPagamento()).isNotNull();
        pedido.setDataPagamento(LocalDateTime.now().minusDays(15).minusMinutes(5));
        pedidoRepository.save(pedido);

        assertThat(get("/pedidos", token(noPrazo.estudante())).corpo().get(0).path("prazoRetirada").asText()).isNotBlank();

        prazosDeRetiradaJob.verificarPrazos();

        assertThat(status(vencida)).isEqualTo(StatusPedido.REEMBOLSADO);
        assertThat(recarregar(vencida.livro()).getEstoqueCompra()).isEqualTo(5);
        verify(mercadoPagoService).reembolsarOrder(eq(vencida.orderId()));
        verify(logService).registrarLog(any(UUID.class), anyString(), eq("PEDIDO_REEMBOLSADO_AUTOMATICO"), contains(vencida.pedidoId().toString()));
        assertThat(status(noPrazo)).isEqualTo(StatusPedido.APROVADO);
    }

    @Test
    void falhaNoReembolsoAutomaticoMantemACompraParaTentarDeNovo() {
        Compra vencida = compraAprovada(5, 1);
        Pedido pedido = pedidoRepository.findById(vencida.pedidoId()).orElseThrow();
        pedido.setDataPagamento(LocalDateTime.now().minusDays(20));
        pedidoRepository.save(pedido);
        doThrow(new RuntimeException("Falha ao solicitar o reembolso")).when(mercadoPagoService).reembolsarOrder(anyString());

        prazosDeRetiradaJob.verificarPrazos();

        assertThat(status(vencida)).isEqualTo(StatusPedido.APROVADO);
        assertThat(recarregar(vencida.livro()).getEstoqueCompra()).isEqualTo(4);
    }
}
