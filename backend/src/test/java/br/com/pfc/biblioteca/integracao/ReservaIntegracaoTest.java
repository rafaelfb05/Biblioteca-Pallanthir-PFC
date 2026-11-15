package br.com.pfc.biblioteca.integracao;

import br.com.pfc.biblioteca.entity.jpa.Livro;
import br.com.pfc.biblioteca.entity.jpa.Usuario;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class ReservaIntegracaoTest extends TesteIntegracaoBase {

    private Resposta reservar(Usuario usuario, Livro livro) {
        return post("/reservas/" + usuario.getId() + "/" + livro.getId(), token(usuario), null);
    }

    private JsonNode reservaDoLivro(Usuario usuario, Livro livro) {
        for (JsonNode reserva : get("/reservas/usuario/" + usuario.getId(), token(usuario)).corpo()) {
            if (reserva.path("livroId").asText().equals(livro.getId().toString())) {
                return reserva;
            }
        }
        throw new AssertionError("Reserva não encontrada");
    }

    @Test
    void reservaDiminuiOEstoqueParaReserva() {
        Usuario estudante = criarEstudante();
        Livro livro = criarLivro(30, 2, 5);

        Resposta resposta = reservar(estudante, livro);

        assertThat(resposta.status()).isEqualTo(200);
        assertThat(recarregar(livro).getEstoqueReserva()).isEqualTo(1);
        assertThat(recarregar(livro).getEstoqueCompra()).isEqualTo(5);
        assertThat(reservaDoLivro(estudante, livro).path("status").asText()).isEqualTo("RESERVADO");
    }

    @Test
    void reservaDeLivroSemEstoqueERecusada() {
        Usuario estudante = criarEstudante();
        Livro livro = criarLivro(30, 0, 3);

        Resposta resposta = reservar(estudante, livro);

        assertThat(resposta.status()).isEqualTo(409);
        assertThat(resposta.mensagem()).isEqualTo("Livro esgotado no momento!");
    }

    @Test
    void estudanteNaoReservaEmNomeDeOutraPessoa() {
        Usuario estudante = criarEstudante();
        Usuario outro = criarEstudante();
        Livro livro = criarLivro(30, 2, 0);

        Resposta resposta = post("/reservas/" + outro.getId() + "/" + livro.getId(), token(estudante), null);

        assertThat(resposta.status()).isEqualTo(400);
        assertThat(recarregar(livro).getEstoqueReserva()).isEqualTo(2);
    }

    @Test
    void funcionarioNaoPodeReservar() {
        Usuario funcionario = criarFuncionario();
        Livro livro = criarLivro(30, 2, 0);

        assertThat(reservar(funcionario, livro).status()).isEqualTo(403);
    }

    @Test
    void cancelamentoDevolveOExemplarAoEstoque() {
        Usuario estudante = criarEstudante();
        Livro livro = criarLivro(30, 1, 0);
        reservar(estudante, livro);
        String reservaId = reservaDoLivro(estudante, livro).path("id").asText();

        Resposta resposta = put("/reservas/" + reservaId + "/cancelar", token(estudante), null);

        assertThat(resposta.status()).isEqualTo(200);
        assertThat(recarregar(livro).getEstoqueReserva()).isEqualTo(1);
        assertThat(reservaDoLivro(estudante, livro).path("status").asText()).isEqualTo("CANCELADO");
    }

    @Test
    void fluxoCompletoDeEntregaEDevolucao() {
        Usuario estudante = criarEstudante();
        Usuario funcionario = criarFuncionario();
        Livro livro = criarLivro(30, 2, 0);
        reservar(estudante, livro);
        String reservaId = reservaDoLivro(estudante, livro).path("id").asText();

        assertThat(put("/reservas/" + reservaId + "/entregar", token(funcionario), null).status()).isEqualTo(200);
        assertThat(reservaDoLivro(estudante, livro).path("status").asText()).isEqualTo("ENTREGUE");
        assertThat(recarregar(livro).getEstoqueReserva()).isEqualTo(1);
        assertThat(put("/reservas/" + reservaId + "/cancelar", token(estudante), null).status()).isEqualTo(400);

        assertThat(put("/reservas/" + reservaId + "/devolver", token(funcionario), null).status()).isEqualTo(200);
        assertThat(reservaDoLivro(estudante, livro).path("status").asText()).isEqualTo("DEVOLVIDO");
        assertThat(recarregar(livro).getEstoqueReserva()).isEqualTo(2);
    }

    @Test
    void devolucaoExigeLivroEntregue() {
        Usuario estudante = criarEstudante();
        Usuario funcionario = criarFuncionario();
        Livro livro = criarLivro(30, 1, 0);
        reservar(estudante, livro);
        String reservaId = reservaDoLivro(estudante, livro).path("id").asText();

        assertThat(put("/reservas/" + reservaId + "/devolver", token(funcionario), null).status()).isEqualTo(400);
        assertThat(recarregar(livro).getEstoqueReserva()).isZero();
    }

    @Test
    void entregaDeReservaCanceladaERecusada() {
        Usuario estudante = criarEstudante();
        Usuario funcionario = criarFuncionario();
        Livro livro = criarLivro(30, 1, 0);
        reservar(estudante, livro);
        String reservaId = reservaDoLivro(estudante, livro).path("id").asText();
        put("/reservas/" + reservaId + "/cancelar", token(estudante), null);

        assertThat(put("/reservas/" + reservaId + "/entregar", token(funcionario), null).status()).isEqualTo(400);
    }

    @Test
    void estudanteNaoRegistraEntrega() {
        Usuario estudante = criarEstudante();
        Livro livro = criarLivro(30, 1, 0);
        reservar(estudante, livro);
        String reservaId = reservaDoLivro(estudante, livro).path("id").asText();

        assertThat(put("/reservas/" + reservaId + "/entregar", token(estudante), null).status()).isEqualTo(403);
    }

    @Test
    void estudanteSoVeAsPropriasReservasEFuncionarioVeQualquerUma() {
        Usuario estudante = criarEstudante();
        Usuario outro = criarEstudante();
        Usuario funcionario = criarFuncionario();
        reservar(outro, criarLivro(30, 1, 0));

        assertThat(get("/reservas/usuario/" + outro.getId(), token(estudante)).status()).isEqualTo(400);
        Resposta doFuncionario = get("/reservas/usuario/" + outro.getId(), token(funcionario));
        assertThat(doFuncionario.status()).isEqualTo(200);
        assertThat(doFuncionario.corpo().size()).isEqualTo(1);
    }

    @Test
    void identificadorInvalidoDevolve400() {
        Usuario estudante = criarEstudante();

        Resposta naUrl = get("/reservas/usuario/10", token(estudante));
        Resposta noCorpo = post("/carrinho/itens", token(estudante), java.util.Map.of("livroId", "10", "quantidade", 1));

        assertThat(naUrl.status()).isEqualTo(400);
        assertThat(naUrl.mensagem()).contains("usuarioId");
        assertThat(noCorpo.status()).isEqualTo(400);
    }

    @Test
    void reservasSimultaneasNaoConsomemOMesmoExemplar() {
        Usuario primeiro = criarEstudante();
        Usuario segundo = criarEstudante();
        Livro livro = criarLivro(30, 1, 0);

        List<Integer> status = List.of(primeiro, segundo).stream()
                .map(usuario -> CompletableFuture.supplyAsync(() -> reservar(usuario, livro).status()))
                .toList()
                .stream()
                .map(CompletableFuture::join)
                .sorted()
                .toList();

        assertThat(status).containsExactly(200, 409);
        assertThat(recarregar(livro).getEstoqueReserva()).isZero();
    }

    private JsonNode reservaEmAberto(String tokenFuncionario, String reservaId) {
        for (JsonNode reserva : get("/reservas/em-aberto", tokenFuncionario).corpo()) {
            if (reserva.path("id").asText().equals(reservaId)) {
                return reserva;
            }
        }
        return null;
    }

    @Test
    void funcionarioVeTodasAsReservasEmAbertoComONomeDoEstudante() {
        Usuario estudante = criarEstudante();
        Usuario funcionario = criarFuncionario();
        String tokenFuncionario = token(funcionario);
        Livro reservado = criarLivro(30, 1, 0);
        Livro entregue = criarLivro(30, 1, 0);
        Livro cancelado = criarLivro(30, 1, 0);
        reservar(estudante, reservado);
        reservar(estudante, entregue);
        reservar(estudante, cancelado);
        String idReservado = reservaDoLivro(estudante, reservado).path("id").asText();
        String idEntregue = reservaDoLivro(estudante, entregue).path("id").asText();
        String idCancelado = reservaDoLivro(estudante, cancelado).path("id").asText();
        put("/reservas/" + idEntregue + "/entregar", tokenFuncionario, null);
        put("/reservas/" + idCancelado + "/cancelar", token(estudante), null);

        JsonNode aberta = reservaEmAberto(tokenFuncionario, idReservado);
        assertThat(aberta).isNotNull();
        assertThat(aberta.path("status").asText()).isEqualTo("RESERVADO");
        assertThat(aberta.path("nomeEstudante").asText()).isEqualTo(estudante.getNome());
        assertThat(aberta.path("usuarioId").asText()).isEqualTo(estudante.getId().toString());
        assertThat(aberta.path("tituloLivro").asText()).isEqualTo(reservado.getTitulo());
        assertThat(reservaEmAberto(tokenFuncionario, idEntregue).path("status").asText()).isEqualTo("ENTREGUE");
        assertThat(reservaEmAberto(tokenFuncionario, idCancelado)).isNull();

        put("/reservas/" + idEntregue + "/devolver", tokenFuncionario, null);
        assertThat(reservaEmAberto(tokenFuncionario, idEntregue)).isNull();
        assertThat(get("/reservas/em-aberto", token(estudante)).status()).isEqualTo(403);
    }
}
