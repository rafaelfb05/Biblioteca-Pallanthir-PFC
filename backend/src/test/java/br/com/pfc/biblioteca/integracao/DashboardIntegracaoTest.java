package br.com.pfc.biblioteca.integracao;

import br.com.pfc.biblioteca.entity.jpa.Livro;
import br.com.pfc.biblioteca.entity.jpa.Usuario;
import br.com.pfc.biblioteca.enums.Materia;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardIntegracaoTest extends TesteIntegracaoBase {

    private long quantidadeDaMateria(JsonNode lista, String materia) {
        for (JsonNode item : lista) {
            if (item.path("nome").asText().equals(materia)) {
                return item.path("quantidade").asLong();
            }
        }
        return 0;
    }

    @Test
    void somenteFuncionarioAcessaADashboard() {
        assertThat(get("/dashboard", token(criarEstudante())).status()).isEqualTo(403);
        assertThat(get("/dashboard", null).status()).isIn(401, 403);
        assertThat(get("/dashboard", token(criarFuncionario())).status()).isEqualTo(200);
    }

    @Test
    void numerosAcompanhamReservasEVendas() {
        String funcionario = token(criarFuncionario());
        JsonNode antes = get("/dashboard", funcionario).corpo();

        Usuario estudante = criarEstudante();
        Livro livro = criarLivro(Materia.MEDICINA, 40, 2, 4);
        post("/reservas/" + estudante.getId() + "/" + livro.getId(), token(estudante), null);
        adicionarAoCarrinho(token(estudante), livro, 2);
        long pedidoId = post("/pedidos/checkout", token(estudante), null).corpo().path("id").asLong();
        String orderId = orderDoPedido(pedidoId);
        definirStatusNoMercadoPago(orderId, "processed");
        notificarWebhook(orderId, SEGREDO_WEBHOOK);

        JsonNode depois = get("/dashboard", funcionario).corpo();

        assertThat(depois.path("estoque").path("titulosAtivos").asLong() - antes.path("estoque").path("titulosAtivos").asLong()).isEqualTo(1);
        assertThat(depois.path("estoque").path("exemplaresParaReserva").asLong() - antes.path("estoque").path("exemplaresParaReserva").asLong()).isEqualTo(1);
        assertThat(depois.path("estoque").path("exemplaresParaCompra").asLong() - antes.path("estoque").path("exemplaresParaCompra").asLong()).isEqualTo(2);
        assertThat(depois.path("reservas").path("reservadas").asLong() - antes.path("reservas").path("reservadas").asLong()).isEqualTo(1);
        assertThat(quantidadeDaMateria(depois.path("materiasMaisCompradas"), "MEDICINA")
                - quantidadeDaMateria(antes.path("materiasMaisCompradas"), "MEDICINA")).isEqualTo(2);
    }
}
