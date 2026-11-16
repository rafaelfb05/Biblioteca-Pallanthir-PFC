package br.com.pfc.biblioteca.integracao;

import br.com.pfc.biblioteca.entity.jpa.Livro;
import br.com.pfc.biblioteca.entity.jpa.Multa;
import br.com.pfc.biblioteca.entity.jpa.Usuario;
import br.com.pfc.biblioteca.service.MercadoPagoService;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;

class MultaIntegracaoTest extends TesteIntegracaoBase {

    @BeforeEach
    void simularPagamentoDeMulta() {
        doAnswer(invocacao -> {
            Multa multa = invocacao.getArgument(0);
            String id = "ORDMULTA" + Math.abs(multa.getId().getMostSignificantBits() % 100000) + "X";
            ordensMercadoPago.put(id, new MercadoPagoService.OrderMP(id, multa.getReferenciaDePagamento(), "created", "created"));
            return new MercadoPagoService.OrderCriada(id, "https://www.mercadopago.com.br/checkout/v1/redirect?order_id=" + id);
        }).when(mercadoPagoService).criarOrderMulta(any(Multa.class), anyInt());
    }

    private String reservaEntregue(Usuario estudante, Livro livro, String tokenFuncionario) {
        post("/reservas/" + estudante.getId() + "/" + livro.getId(), token(estudante), null);
        String reservaId = get("/reservas/usuario/" + estudante.getId(), token(estudante)).corpo().get(0).path("id").asText();
        put("/reservas/" + reservaId + "/entregar", tokenFuncionario, null);
        return reservaId;
    }

    private Resposta devolver(String reservaId, String tokenFuncionario, Boolean danificado, Object valor) {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("danificado", danificado);
        corpo.put("valorMulta", valor);
        corpo.put("observacao", danificado ? "Capa rasgada" : null);
        return put("/reservas/" + reservaId + "/devolver", tokenFuncionario, corpo);
    }

    private JsonNode primeiraMulta(Usuario estudante) {
        return get("/multas/minhas", token(estudante)).corpo().get(0);
    }

    @Test
    void devolucaoComDanoGeraMultaNoValorDoLivroEOLivroSaiDeCirculacao() {
        Usuario estudante = criarEstudante();
        String funcionario = token(criarFuncionario());
        Livro livro = criarLivro(89.9, 1, 0);
        String reservaId = reservaEntregue(estudante, livro, funcionario);

        Resposta devolucao = devolver(reservaId, funcionario, true, null);

        assertThat(devolucao.status()).isEqualTo(200);
        assertThat(recarregar(livro).getEstoqueReserva()).isZero();
        JsonNode multa = primeiraMulta(estudante);
        assertThat(multa.path("status").asText()).isEqualTo("PENDENTE");
        assertThat(new BigDecimal(multa.path("valor").asText())).isEqualByComparingTo("89.90");
        assertThat(multa.path("motivo").asText()).isEqualTo("Capa rasgada");
        assertThat(multa.path("tituloLivro").asText()).isEqualTo(livro.getTitulo());
        assertThat(get("/reservas/usuario/" + estudante.getId(), token(estudante)).corpo().get(0).path("status").asText()).isEqualTo("DEVOLVIDO");
    }

    @Test
    void devolucaoEmBomEstadoNaoGeraMultaEVoltaAoEstoque() {
        Usuario estudante = criarEstudante();
        String funcionario = token(criarFuncionario());
        Livro livro = criarLivro(50, 1, 0);
        String reservaId = reservaEntregue(estudante, livro, funcionario);

        assertThat(devolver(reservaId, funcionario, false, null).status()).isEqualTo(200);
        assertThat(put("/reservas/" + reservaId + "/devolver", funcionario, null).status()).isEqualTo(400);

        assertThat(recarregar(livro).getEstoqueReserva()).isEqualTo(1);
        assertThat(get("/multas/minhas", token(estudante)).corpo().size()).isZero();
    }

    @Test
    void livroSemPrecoExigeOValorDaMulta() {
        Usuario estudante = criarEstudante();
        String funcionario = token(criarFuncionario());
        Livro livro = criarLivro(0, 1, 0);
        String reservaId = reservaEntregue(estudante, livro, funcionario);

        Resposta semValor = devolver(reservaId, funcionario, true, null);
        assertThat(semValor.status()).isEqualTo(400);
        assertThat(semValor.mensagem()).contains("Informe o valor da multa");

        assertThat(devolver(reservaId, funcionario, true, 35).status()).isEqualTo(200);
        assertThat(new BigDecimal(primeiraMulta(estudante).path("valor").asText())).isEqualByComparingTo("35.00");
    }

    @Test
    void multaPendenteImpedeReservarEComprar() {
        Usuario estudante = criarEstudante();
        String funcionario = token(criarFuncionario());
        devolver(reservaEntregue(estudante, criarLivro(40, 1, 0), funcionario), funcionario, true, null);
        Livro outro = criarLivro(30, 2, 3);

        Resposta reserva = post("/reservas/" + estudante.getId() + "/" + outro.getId(), token(estudante), null);
        adicionarAoCarrinho(token(estudante), outro, 1);
        Resposta compra = post("/pedidos/checkout", token(estudante), null);

        assertThat(reserva.status()).isEqualTo(409);
        assertThat(reserva.mensagem()).contains("multa pendente");
        assertThat(compra.status()).isEqualTo(409);
        assertThat(compra.mensagem()).contains("multa pendente");
        assertThat(recarregar(outro).getEstoqueReserva()).isEqualTo(2);
        assertThat(recarregar(outro).getEstoqueCompra()).isEqualTo(3);
    }

    @Test
    void pagamentoDaMultaPeloMercadoPagoLiberaOEstudante() {
        Usuario estudante = criarEstudante();
        String funcionario = token(criarFuncionario());
        devolver(reservaEntregue(estudante, criarLivro(40, 1, 0), funcionario), funcionario, true, null);
        String multaId = primeiraMulta(estudante).path("id").asText();

        Resposta pagamento = post("/multas/" + multaId + "/pagar", token(estudante), null);
        assertThat(pagamento.status()).isEqualTo(200);
        assertThat(pagamento.corpo().path("linkPagamento").asText()).contains("mercadopago.com.br");
        String orderId = ordensMercadoPago.keySet().stream().filter(id -> id.startsWith("ORDMULTA")).findFirst().orElseThrow();
        assertThat(post("/multas/" + multaId + "/pagar", token(estudante), null).corpo().path("linkPagamento").asText())
                .isEqualTo(pagamento.corpo().path("linkPagamento").asText());

        definirStatusNoMercadoPago(orderId, "processed");
        assertThat(notificarWebhook(orderId, SEGREDO_WEBHOOK).status()).isEqualTo(200);

        assertThat(primeiraMulta(estudante).path("status").asText()).isEqualTo("PAGA");
        Livro outro = criarLivro(30, 1, 0);
        assertThat(post("/reservas/" + estudante.getId() + "/" + outro.getId(), token(estudante), null).status()).isEqualTo(200);
    }

    @Test
    void funcionarioListaEDispensaMulta() {
        Usuario estudante = criarEstudante();
        String funcionario = token(criarFuncionario());
        devolver(reservaEntregue(estudante, criarLivro(40, 1, 0), funcionario), funcionario, true, null);
        String multaId = primeiraMulta(estudante).path("id").asText();

        boolean listada = false;
        for (JsonNode multa : get("/multas", funcionario).corpo()) {
            listada |= multa.path("id").asText().equals(multaId)
                    && multa.path("nomeEstudante").asText().equals(estudante.getNome());
        }
        assertThat(listada).isTrue();
        assertThat(put("/multas/" + multaId + "/dispensar", token(estudante), null).status()).isEqualTo(403);
        assertThat(get("/multas", token(estudante)).status()).isEqualTo(403);

        assertThat(put("/multas/" + multaId + "/dispensar", funcionario, null).status()).isEqualTo(200);
        assertThat(primeiraMulta(estudante).path("status").asText()).isEqualTo("DISPENSADA");
        assertThat(put("/multas/" + multaId + "/dispensar", funcionario, null).status()).isEqualTo(409);
    }

    @Test
    void estudanteNaoPagaMultaDeOutraPessoaENaoExcluiContaComMulta() {
        Usuario estudante = criarEstudante();
        String funcionario = token(criarFuncionario());
        devolver(reservaEntregue(estudante, criarLivro(40, 1, 0), funcionario), funcionario, true, null);
        String multaId = primeiraMulta(estudante).path("id").asText();

        assertThat(post("/multas/" + multaId + "/pagar", token(criarEstudante()), null).status()).isEqualTo(404);
        Resposta exclusao = delete("/usuarios/deletar/" + estudante.getId(), token(estudante));
        assertThat(exclusao.status()).isEqualTo(409);
        assertThat(exclusao.mensagem()).contains("multa pendente");
    }
}
