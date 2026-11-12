package br.com.pfc.biblioteca.integracao;

import br.com.pfc.biblioteca.entity.jpa.Livro;
import br.com.pfc.biblioteca.entity.jpa.Pedido;
import br.com.pfc.biblioteca.entity.jpa.Usuario;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class CarrinhoPedidoIntegracaoTest extends TesteIntegracaoBase {

    @Test
    void carrinhoCalculaSubtotaisETotal() {
        Usuario estudante = criarEstudante();
        Livro livro = criarLivro(50.5, 0, 5);

        Resposta resposta = adicionarAoCarrinho(token(estudante), livro, 2);

        assertThat(resposta.status()).isEqualTo(200);
        assertThat(resposta.corpo().path("itens").size()).isEqualTo(1);
        assertThat(new BigDecimal(resposta.corpo().path("total").asText())).isEqualByComparingTo("101.00");
    }

    @Test
    void adicionarDeNovoDefineANovaQuantidade() {
        Usuario estudante = criarEstudante();
        Livro livro = criarLivro(10, 0, 5);
        adicionarAoCarrinho(token(estudante), livro, 1);

        Resposta resposta = adicionarAoCarrinho(token(estudante), livro, 3);

        assertThat(resposta.corpo().path("itens").size()).isEqualTo(1);
        assertThat(resposta.corpo().path("itens").get(0).path("quantidade").asInt()).isEqualTo(3);
    }

    @Test
    void quantidadeNaoPassaDoEstoqueParaVenda() {
        Usuario estudante = criarEstudante();
        Livro livro = criarLivro(10, 5, 2);

        assertThat(adicionarAoCarrinho(token(estudante), livro, 3).status()).isEqualTo(409);
        assertThat(get("/carrinho", token(estudante)).corpo().path("itens").size()).isZero();
    }

    @Test
    void livroSemPrecoNaoPodeSerComprado() {
        Usuario estudante = criarEstudante();
        Livro livro = criarLivro(0, 1, 5);

        assertThat(adicionarAoCarrinho(token(estudante), livro, 1).status()).isEqualTo(400);
    }

    @Test
    void removerItemRecalculaOCarrinho() {
        Usuario estudante = criarEstudante();
        Livro primeiro = criarLivro(10, 0, 5);
        Livro segundo = criarLivro(20, 0, 5);
        adicionarAoCarrinho(token(estudante), primeiro, 1);
        adicionarAoCarrinho(token(estudante), segundo, 1);

        Resposta resposta = delete("/carrinho/itens/" + primeiro.getId(), token(estudante));

        assertThat(resposta.corpo().path("itens").size()).isEqualTo(1);
        assertThat(new BigDecimal(resposta.corpo().path("total").asText())).isEqualByComparingTo("20.00");
    }

    @Test
    void somenteEstudanteUsaCarrinhoEPedidos() {
        Usuario funcionario = criarFuncionario();

        assertThat(get("/carrinho", token(funcionario)).status()).isEqualTo(403);
        assertThat(get("/pedidos", token(funcionario)).status()).isEqualTo(403);
        assertThat(get("/carrinho", null).status()).isIn(401, 403);
    }

    @Test
    void checkoutCriaPedidoSeparaEstoqueEMantemOCarrinho() {
        Usuario estudante = criarEstudante();
        Livro livro = criarLivro(50, 1, 5);
        adicionarAoCarrinho(token(estudante), livro, 2);

        Resposta resposta = post("/pedidos/checkout", token(estudante), null);

        assertThat(resposta.status()).isEqualTo(200);
        assertThat(resposta.corpo().path("status").asText()).isEqualTo("AGUARDANDO_PAGAMENTO");
        assertThat(resposta.corpo().path("linkPagamento").asText()).contains("mercadopago.com.br");
        assertThat(new BigDecimal(resposta.corpo().path("total").asText())).isEqualByComparingTo("100.00");
        assertThat(resposta.corpo().path("dataExpiracao").asText()).isNotBlank();
        assertThat(recarregar(livro).getEstoqueCompra()).isEqualTo(3);
        assertThat(recarregar(livro).getEstoqueReserva()).isEqualTo(1);
        assertThat(get("/carrinho", token(estudante)).corpo().path("itens").size()).isEqualTo(1);
        verify(mercadoPagoService, times(1)).criarOrder(any(Pedido.class), anyInt());
    }

    @Test
    void precoDoPedidoVemDoBancoNoMomentoDoCheckout() {
        Usuario estudante = criarEstudante();
        Livro livro = criarLivro(50, 0, 5);
        adicionarAoCarrinho(token(estudante), livro, 2);
        Livro alterado = recarregar(livro);
        alterado.setPreco(80.0);
        livroRepository.save(alterado);

        Resposta resposta = post("/pedidos/checkout", token(estudante), null);

        assertThat(new BigDecimal(resposta.corpo().path("total").asText())).isEqualByComparingTo("160.00");
    }

    @Test
    void checkoutComCarrinhoVazioERecusado() {
        Usuario estudante = criarEstudante();

        assertThat(post("/pedidos/checkout", token(estudante), null).status()).isEqualTo(400);
        verify(mercadoPagoService, never()).criarOrder(any(Pedido.class), anyInt());
    }

    @Test
    void pedidoPendenteBloqueiaCarrinhoENovoCheckout() {
        Usuario estudante = criarEstudante();
        Livro livro = criarLivro(10, 0, 5);
        Livro outro = criarLivro(10, 0, 5);
        adicionarAoCarrinho(token(estudante), livro, 1);
        post("/pedidos/checkout", token(estudante), null);

        assertThat(post("/pedidos/checkout", token(estudante), null).status()).isEqualTo(409);
        assertThat(adicionarAoCarrinho(token(estudante), outro, 1).status()).isEqualTo(409);
        assertThat(delete("/carrinho/itens/" + livro.getId(), token(estudante)).status()).isEqualTo(409);
        assertThat(recarregar(livro).getEstoqueCompra()).isEqualTo(4);
    }

    @Test
    void falhaNoMercadoPagoDesfazOPedidoInteiro() {
        Usuario estudante = criarEstudante();
        Livro livro = criarLivro(10, 0, 5);
        adicionarAoCarrinho(token(estudante), livro, 2);
        doThrow(new RuntimeException("Falha ao criar o pagamento")).when(mercadoPagoService).criarOrder(any(Pedido.class), anyInt());

        Resposta resposta = post("/pedidos/checkout", token(estudante), null);

        assertThat(resposta.status()).isEqualTo(500);
        assertThat(recarregar(livro).getEstoqueCompra()).isEqualTo(5);
        assertThat(get("/pedidos", token(estudante)).corpo().size()).isZero();
        assertThat(get("/carrinho", token(estudante)).corpo().path("itens").size()).isEqualTo(1);
    }

    @Test
    void estudanteSoConsultaOsPropriosPedidos() {
        Usuario dono = criarEstudante();
        Usuario outro = criarEstudante();
        adicionarAoCarrinho(token(dono), criarLivro(10, 0, 5), 1);
        String pedidoId = post("/pedidos/checkout", token(dono), null).corpo().path("id").asText();

        assertThat(get("/pedidos/" + pedidoId, token(dono)).status()).isEqualTo(200);
        assertThat(get("/pedidos/" + pedidoId, token(outro)).status()).isEqualTo(400);
        assertThat(get("/pedidos", token(outro)).corpo().size()).isZero();
    }
}
