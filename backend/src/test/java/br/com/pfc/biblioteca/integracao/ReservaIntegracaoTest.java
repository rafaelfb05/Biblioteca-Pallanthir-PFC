package br.com.pfc.biblioteca.integracao;

import br.com.pfc.biblioteca.entity.jpa.Livro;
import br.com.pfc.biblioteca.entity.jpa.Reserva;
import br.com.pfc.biblioteca.entity.jpa.Usuario;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import br.com.pfc.biblioteca.schedule.PrazosDeRetiradaJob;
import br.com.pfc.biblioteca.service.ReservaService;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class ReservaIntegracaoTest extends TesteIntegracaoBase {

    @Autowired
    private PrazosDeRetiradaJob prazosDeRetiradaJob;

    @Autowired
    private ReservaService reservaService;

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

    @Test
    void reservaNaoRetiradaEmTresDiasECanceladaAutomaticamente() {
        Usuario estudante = criarEstudante();
        Livro vencido = criarLivro(30, 1, 0);
        Livro noPrazo = criarLivro(30, 1, 0);
        reservar(estudante, vencido);
        reservar(estudante, noPrazo);
        UUID idVencida = UUID.fromString(reservaDoLivro(estudante, vencido).path("id").asText());
        Reserva antiga = reservaRepository.findById(idVencida).orElseThrow();
        antiga.setDataReserva(LocalDateTime.now().minusDays(3).minusMinutes(5));
        reservaRepository.save(antiga);

        assertThat(reservaDoLivro(estudante, noPrazo).path("prazoRetirada").asText()).isNotBlank();
        assertThat(reservaService.reservasComPrazoVencido()).contains(idVencida);

        prazosDeRetiradaJob.verificarPrazos();

        assertThat(reservaDoLivro(estudante, vencido).path("status").asText()).isEqualTo("CANCELADO");
        assertThat(reservaDoLivro(estudante, vencido).path("prazoRetirada").isNull()).isTrue();
        assertThat(recarregar(vencido).getEstoqueReserva()).isEqualTo(1);
        assertThat(reservaDoLivro(estudante, noPrazo).path("status").asText()).isEqualTo("RESERVADO");
        assertThat(recarregar(noPrazo).getEstoqueReserva()).isZero();
    }

    @Test
    void reservaJaEntregueNaoECanceladaPeloPrazo() {
        Usuario estudante = criarEstudante();
        Livro livro = criarLivro(30, 1, 0);
        reservar(estudante, livro);
        String id = reservaDoLivro(estudante, livro).path("id").asText();
        put("/reservas/" + id + "/entregar", token(criarFuncionario()), null);
        Reserva entregue = reservaRepository.findById(UUID.fromString(id)).orElseThrow();
        entregue.setDataReserva(LocalDateTime.now().minusDays(10));
        reservaRepository.save(entregue);

        prazosDeRetiradaJob.verificarPrazos();

        assertThat(reservaDoLivro(estudante, livro).path("status").asText()).isEqualTo("ENTREGUE");
    }

    @Test
    void reservaRegistraAVersaoAceitaDaPoliticaDeReservas() {
        Usuario estudante = criarEstudante();
        Livro livro = criarLivro(30, 2, 0);

        String vigente = documentoService.versaoVigenteDaPoliticaDeReservas();
        Resposta resposta = post("/reservas/" + estudante.getId() + "/" + livro.getId() + "?politicaVersao=" + vigente, token(estudante), null);

        assertThat(resposta.status()).isEqualTo(200);
        Reserva salva = reservaRepository.findById(UUID.fromString(reservaDoLivro(estudante, livro).path("id").asText())).orElseThrow();
        assertThat(salva.getPoliticaVersao()).isEqualTo(vigente);
        assertThat(salva.getDataAceitePolitica()).isNotNull();
        assertThat(post("/reservas/" + estudante.getId() + "/" + criarLivro(30, 2, 0).getId() + "?politicaVersao=" + "x".repeat(70),
                token(estudante), null).status()).isEqualTo(400);
    }

    @Test
    void entregaDaPrazoDeSeteDiasParaDevolverEMostraACapa() {
        Usuario estudante = criarEstudante();
        Livro livro = criarLivro(30, 1, 0);
        livro.setCapaUrl("https://covers.openlibrary.org/b/isbn/1-L.jpg");
        livroRepository.save(livro);
        reservar(estudante, livro);
        assertThat(reservaDoLivro(estudante, livro).path("capaUrl").asText()).isEqualTo("https://covers.openlibrary.org/b/isbn/1-L.jpg");
        assertThat(reservaDoLivro(estudante, livro).path("prazoDevolucao").isNull()).isTrue();
        String id = reservaDoLivro(estudante, livro).path("id").asText();

        put("/reservas/" + id + "/entregar", token(criarFuncionario()), null);

        JsonNode entregue = reservaDoLivro(estudante, livro);
        LocalDateTime dataEntrega = LocalDateTime.parse(entregue.path("dataEntrega").asText());
        LocalDateTime prazo = LocalDateTime.parse(entregue.path("prazoDevolucao").asText());
        assertThat(prazo).isEqualTo(dataEntrega.plusDays(7));
        assertThat(entregue.path("prazoRetirada").isNull()).isTrue();
    }

    @Test
    void devolucaoAtrasadaBloqueiaNovasReservasAteODevolverOLivro() {
        Usuario estudante = criarEstudante();
        String funcionario = token(criarFuncionario());
        Livro emprestado = criarLivro(30, 1, 0);
        reservar(estudante, emprestado);
        String id = reservaDoLivro(estudante, emprestado).path("id").asText();
        put("/reservas/" + id + "/entregar", funcionario, null);
        Livro outro = criarLivro(30, 2, 0);

        assertThat(reservar(estudante, outro).status()).isEqualTo(200);

        Reserva entregue = reservaRepository.findById(UUID.fromString(id)).orElseThrow();
        entregue.setDataEntrega(LocalDateTime.now().minusDays(8));
        reservaRepository.save(entregue);
        Livro maisUm = criarLivro(30, 2, 0);

        Resposta bloqueada = reservar(estudante, maisUm);
        assertThat(bloqueada.status()).isEqualTo(409);
        assertThat(bloqueada.mensagem()).contains("devolução").contains(emprestado.getTitulo());
        assertThat(recarregar(maisUm).getEstoqueReserva()).isEqualTo(2);

        put("/reservas/" + id + "/devolver", funcionario, null);
        assertThat(reservar(estudante, maisUm).status()).isEqualTo(200);
    }

    @Test
    void reservaExigeAPoliticaVigenteELivroAtivo() {
        Usuario estudante = criarEstudante();
        String token = token(estudante);
        Livro livro = criarLivro(30, 3, 0);

        assertThat(post("/reservas/" + estudante.getId() + "/" + livro.getId() + "?politicaVersao=versao-antiga", token, null).status()).isEqualTo(400);

        livro.setAtivo(false);
        livroRepository.save(livro);
        assertThat(post("/reservas/" + estudante.getId() + "/" + livro.getId(), token, null).status()).isEqualTo(404);
        assertThat(recarregar(livro).getEstoqueReserva()).isEqualTo(3);
    }

    @Test
    void estudanteTemUmaReservaPorLivroEAteTresEmAberto() {
        Usuario estudante = criarEstudante();
        String token = token(estudante);
        Livro primeiro = criarLivro(30, 5, 0);

        assertThat(post("/reservas/" + estudante.getId() + "/" + primeiro.getId(), token, null).status()).isEqualTo(200);
        Resposta repetida = post("/reservas/" + estudante.getId() + "/" + primeiro.getId(), token, null);
        assertThat(repetida.status()).isEqualTo(409);
        assertThat(recarregar(primeiro).getEstoqueReserva()).isEqualTo(4);

        assertThat(post("/reservas/" + estudante.getId() + "/" + criarLivro(30, 5, 0).getId(), token, null).status()).isEqualTo(200);
        assertThat(post("/reservas/" + estudante.getId() + "/" + criarLivro(30, 5, 0).getId(), token, null).status()).isEqualTo(200);
        Resposta quarta = post("/reservas/" + estudante.getId() + "/" + criarLivro(30, 5, 0).getId(), token, null);
        assertThat(quarta.status()).isEqualTo(409);
        assertThat(quarta.mensagem()).contains("3 reservas em aberto");
    }

    @Test
    void termosDesatualizadosBloqueiamReservaECompra() {
        Usuario estudante = criarEstudante();
        estudante.setVersaoTermos("versao-antiga");
        usuarioRepository.save(estudante);
        String token = token(estudante);
        Livro livro = criarLivro(30, 3, 3);

        Resposta reserva = post("/reservas/" + estudante.getId() + "/" + livro.getId(), token, null);
        assertThat(reserva.status()).isEqualTo(409);
        assertThat(reserva.mensagem()).contains("Aceite a nova versão");

        adicionarAoCarrinho(token, livro, 1);
        assertThat(post("/pedidos/checkout", token, null).status()).isEqualTo(409);
    }

    @Test
    void livroNaoAceitaPrecoNemEstoqueNegativos() {
        String funcionario = token(criarFuncionario());
        Livro livro = criarLivro(30, 3, 3);

        assertThat(put("/livros/atualizar/" + livro.getId(), funcionario, java.util.Map.of("estoqueReserva", -5)).status()).isEqualTo(400);
        assertThat(put("/livros/atualizar/" + livro.getId(), funcionario, java.util.Map.of("preco", -1.0)).status()).isEqualTo(400);
        assertThat(recarregar(livro).getEstoqueReserva()).isEqualTo(3);
    }
}

