package br.com.pfc.biblioteca.integracao;

import br.com.pfc.biblioteca.entity.jpa.Livro;
import br.com.pfc.biblioteca.entity.jpa.Pedido;
import br.com.pfc.biblioteca.entity.jpa.Usuario;
import br.com.pfc.biblioteca.enums.Materia;
import br.com.pfc.biblioteca.enums.TipoUsuario;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GerenteIntegracaoTest extends TesteIntegracaoBase {

    private Usuario criarGerente() {
        Usuario gerente = criarUsuario(TipoUsuario.GERENTE);
        gerente.setNome("Gerente " + UUID.randomUUID().toString().substring(0, 6));
        return usuarioRepository.save(gerente);
    }

    @Test
    void somenteGerenteConsultaLogsEVendasEGerenteTambemTemAcessoDeFuncionario() {
        String estudante = token(criarEstudante());
        String funcionario = token(criarFuncionario());
        String gerente = token(criarGerente());

        assertThat(get("/logs", estudante).status()).isEqualTo(403);
        assertThat(get("/logs", funcionario).status()).isEqualTo(403);
        assertThat(get("/logs", gerente).status()).isEqualTo(200);

        assertThat(get("/dashboard/vendas", funcionario).status()).isEqualTo(403);
        assertThat(get("/dashboard/vendas", gerente).status()).isEqualTo(200);

        assertThat(get("/dashboard", funcionario).status()).isEqualTo(200);
        assertThat(get("/dashboard", gerente).status()).isEqualTo(200);
        assertThat(get("/reservas/em-aberto", gerente).status()).isEqualTo(200);
        assertThat(get("/multas", gerente).status()).isEqualTo(200);
    }

    @Test
    void naoEPossivelSeCadastrarComoGerente() {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("nome", "Tentativa");
        corpo.put("email", emailUnico());
        corpo.put("senha", SENHA);
        corpo.put("tipo", "GERENTE");
        corpo.put("aceitouTermos", true);

        Resposta resposta = post("/usuarios/cadastro", null, corpo);

        assertThat(resposta.status()).isEqualTo(400);
        assertThat(resposta.mensagem()).contains("gerente");
    }

    @Test
    void documentosSaoPublicosESoOGerenteEdita() {
        JsonNode documentos = get("/documentos", null).corpo();
        assertThat(documentos).hasSize(3);
        documentos.forEach(documento -> assertThat(documento.path("conteudo").asText()).isNotBlank());

        String original = get("/documentos/POLITICA_DE_RESERVAS", null).corpo().path("conteudo").asText();
        Usuario gerente = criarGerente();
        String token = token(gerente);

        assertThat(put("/documentos/POLITICA_DE_RESERVAS", token(criarFuncionario()), Map.of("conteudo", "Nova")).status()).isEqualTo(403);
        assertThat(put("/documentos/POLITICA_DE_RESERVAS", token, Map.of("conteudo", "   ")).status()).isEqualTo(400);
        assertThat(get("/documentos/INEXISTENTE", null).status()).isEqualTo(400);

        Resposta atualizado = put("/documentos/POLITICA_DE_RESERVAS", token, Map.of("conteudo", "# Política\r\n\r\nTexto novo.  "));
        assertThat(atualizado.status()).isEqualTo(200);
        assertThat(atualizado.corpo().path("atualizadoPor").asText()).isEqualTo(gerente.getNome());

        JsonNode lido = get("/documentos/POLITICA_DE_RESERVAS", null).corpo();
        assertThat(lido.path("conteudo").asText()).isEqualTo("# Política\n\nTexto novo.\n");
        assertThat(lido.path("titulo").asText()).isEqualTo("Política de Uso de Reservas");

        put("/documentos/POLITICA_DE_RESERVAS", token, Map.of("conteudo", original));
    }

    @Test
    void gerentePromoveERebaixaFuncionarios() {
        String gerente = token(criarGerente());
        Usuario funcionario = criarFuncionario();

        Resposta promovido = put("/usuarios/" + funcionario.getId() + "/perfil", gerente, Map.of("tipo", "GERENTE"));
        assertThat(promovido.status()).isEqualTo(200);
        assertThat(promovido.corpo().path("tipo").asText()).isEqualTo("GERENTE");
        Usuario recarregado = usuarioRepository.findById(funcionario.getId()).orElseThrow();
        assertThat(get("/logs", token(recarregado)).status()).isEqualTo(200);

        assertThat(put("/usuarios/" + funcionario.getId() + "/perfil", gerente, Map.of("tipo", "FUNCIONARIO")).status()).isEqualTo(200);
        assertThat(usuarioRepository.findById(funcionario.getId()).orElseThrow().getTipo()).isEqualTo(TipoUsuario.FUNCIONARIO);

        assertThat(put("/usuarios/" + funcionario.getId() + "/perfil", gerente, Map.of("tipo", "ESTUDANTE")).status()).isEqualTo(400);
        assertThat(put("/usuarios/" + criarEstudante().getId() + "/perfil", gerente, Map.of("tipo", "FUNCIONARIO")).status()).isEqualTo(400);
        assertThat(put("/usuarios/" + funcionario.getId() + "/perfil", token(funcionario), Map.of("tipo", "GERENTE")).status()).isEqualTo(403);
    }

    @Test
    void gerenteNaoAlteraNemExcluiAPropriaConta() {
        Usuario gerente = criarGerente();
        String token = token(gerente);

        assertThat(put("/usuarios/" + gerente.getId() + "/perfil", token, Map.of("tipo", "FUNCIONARIO")).status()).isEqualTo(400);
        assertThat(delete("/usuarios/funcionarios/" + gerente.getId(), token).status()).isEqualTo(400);
        assertThat(usuarioRepository.findById(gerente.getId()).orElseThrow().getTipo()).isEqualTo(TipoUsuario.GERENTE);
    }

    @Test
    void gerenteExcluiContaDeFuncionario() {
        String gerente = token(criarGerente());
        Usuario funcionario = criarFuncionario();

        assertThat(delete("/usuarios/funcionarios/" + funcionario.getId(), token(criarFuncionario())).status()).isEqualTo(403);
        assertThat(delete("/usuarios/funcionarios/" + funcionario.getId(), gerente).status()).isEqualTo(204);

        Usuario excluido = usuarioRepository.findById(funcionario.getId()).orElseThrow();
        assertThat(excluido.isAtivo()).isFalse();
        assertThat(excluido.getNome()).isEqualTo("Usuário removido");
        assertThat(excluido.getEmail()).endsWith("@anonimizado.local");

        assertThat(delete("/usuarios/funcionarios/" + criarGerente().getId(), gerente).status()).isEqualTo(400);
        assertThat(delete("/usuarios/funcionarios/" + criarEstudante().getId(), gerente).status()).isEqualTo(400);
        assertThat(delete("/usuarios/funcionarios/" + funcionario.getId(), gerente).status()).isEqualTo(404);
    }

    @Test
    void dashboardDeVendasResumeOsPedidosDoPeriodo() {
        Usuario estudante = criarEstudante();
        Livro livro = criarLivro(Materia.DIREITO, 25, 0, 5);
        adicionarAoCarrinho(token(estudante), livro, 2);
        String pedidoId = post("/pedidos/checkout", token(estudante), null).corpo().path("id").asText();
        String orderId = orderDoPedido(pedidoId);
        definirStatusNoMercadoPago(orderId, "processed");
        notificarWebhook(orderId, SEGREDO_WEBHOOK);

        Pedido pedido = pedidoRepository.findById(UUID.fromString(pedidoId)).orElseThrow();
        pedido.setDataCriacao(LocalDateTime.of(2001, 3, 15, 10, 0));
        pedidoRepository.save(pedido);

        String gerente = token(criarGerente());
        JsonNode vendas = get("/dashboard/vendas?de=2001-03-15&ate=2001-03-15", gerente).corpo();

        assertThat(vendas.path("resumo").path("faturamento").decimalValue()).isEqualByComparingTo("50");
        assertThat(vendas.path("resumo").path("pedidosPagos").asLong()).isEqualTo(1);
        assertThat(vendas.path("resumo").path("ticketMedio").decimalValue()).isEqualByComparingTo("50");
        assertThat(vendas.path("resumo").path("exemplaresVendidos").asLong()).isEqualTo(2);
        assertThat(vendas.path("resumo").path("aguardandoRetirada").asLong()).isEqualTo(1);
        assertThat(vendas.path("pedidosPorStatus").path("APROVADO").asLong()).isEqualTo(1);
        assertThat(vendas.path("livrosMaisVendidos").get(0).path("rotulo").asText()).isEqualTo(livro.getTitulo());
        assertThat(vendas.path("livrosMaisVendidos").get(0).path("quantidade").asLong()).isEqualTo(2);
        assertThat(vendas.path("materiasMaisVendidas").get(0).path("nome").asText()).isEqualTo("DIREITO");
        assertThat(vendas.path("vendasPorMes").get(0).path("mes").asText()).isEqualTo("2001-03");

        assertThat(get("/dashboard/vendas?de=2001-03-16&ate=2001-03-15", gerente).status()).isEqualTo(400);
    }

    @Test
    void cadaAlteracaoGuardaAVersaoAnteriorNoHistorico() {
        Usuario gerente = criarGerente();
        String token = token(gerente);
        String caminho = "/documentos/POLITICA_DE_PRIVACIDADE";
        JsonNode atual = get(caminho, null).corpo();
        String original = atual.path("conteudo").asText();
        long versaoInicial = atual.path("versao").asLong();
        int historicoInicial = get(caminho + "/versoes", token).corpo().size();

        put(caminho, token, Map.of("conteudo", "# Privacidade\n\nPrimeira revisão."));
        Resposta segunda = put(caminho, token, Map.of("conteudo", "# Privacidade\n\nSegunda revisão."));
        assertThat(segunda.corpo().path("versao").asLong()).isEqualTo(versaoInicial + 2);

        JsonNode historico = get(caminho + "/versoes", token).corpo();
        assertThat(historico).hasSize(historicoInicial + 2);
        assertThat(historico.get(0).path("numero").asLong()).isEqualTo(versaoInicial + 1);
        assertThat(historico.get(0).path("publicadoPor").asText()).isEqualTo(gerente.getNome());
        assertThat(historico.get(0).path("conteudo").isNull()).isTrue();
        assertThat(historico.get(1).path("numero").asLong()).isEqualTo(versaoInicial);

        String idDaPrimeiraRevisao = historico.get(0).path("id").asText();
        JsonNode primeiraRevisao = get("/documentos/versoes/" + idDaPrimeiraRevisao, token).corpo();
        assertThat(primeiraRevisao.path("conteudo").asText()).isEqualTo("# Privacidade\n\nPrimeira revisão.\n");
        assertThat(get("/documentos/versoes/" + historico.get(1).path("id").asText(), token).corpo().path("conteudo").asText())
                .isEqualTo(original);

        assertThat(get(caminho + "/versoes", token(criarFuncionario())).status()).isEqualTo(403);
        assertThat(get("/documentos/versoes/" + idDaPrimeiraRevisao, null).status()).isIn(401, 403);

        put(caminho, token, Map.of("conteudo", original));
    }
}
