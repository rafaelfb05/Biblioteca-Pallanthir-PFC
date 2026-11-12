package br.com.pfc.biblioteca.integracao;

import br.com.pfc.biblioteca.entity.jpa.Livro;
import br.com.pfc.biblioteca.entity.jpa.Usuario;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AvaliacaoIntegracaoTest extends TesteIntegracaoBase {

    private Resposta avaliar(Usuario usuario, Livro livro, Object nota, String comentario) {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("nota", nota);
        corpo.put("comentario", comentario);
        return post("/avaliacoes/livro/" + livro.getId(), token(usuario), corpo);
    }

    private JsonNode resumo(Livro livro) {
        return get("/avaliacoes/livro/" + livro.getId(), null).corpo();
    }

    private boolean rankingContem(Livro livro) {
        for (JsonNode item : get("/avaliacoes/ranking", null).corpo()) {
            if (item.path("id").asText().equals(livro.getId().toString())) {
                return true;
            }
        }
        return false;
    }

    @Test
    void mediaETotalConsideramTodasAsAvaliacoes() {
        Livro livro = criarLivro(30, 1, 0);
        Usuario primeiro = criarEstudante();

        assertThat(avaliar(primeiro, livro, 4, "Bom").status()).isEqualTo(200);
        assertThat(avaliar(criarEstudante(), livro, 5, "Ótimo").status()).isEqualTo(200);

        JsonNode resumo = resumo(livro);
        assertThat(resumo.path("media").asDouble()).isEqualTo(4.5);
        assertThat(resumo.path("total").asInt()).isEqualTo(2);
        assertThat(resumo.path("avaliacoes").get(0).path("nomeUsuario").asText()).isNotBlank();
    }

    @Test
    void novaAvaliacaoDoMesmoEstudanteSubstituiAAnterior() {
        Livro livro = criarLivro(30, 1, 0);
        Usuario estudante = criarEstudante();

        avaliar(estudante, livro, 3, "Regular");
        avaliar(estudante, livro, 5, "Mudei de ideia");

        JsonNode resumo = resumo(livro);
        assertThat(resumo.path("total").asInt()).isEqualTo(1);
        assertThat(resumo.path("media").asDouble()).isEqualTo(5.0);
        assertThat(resumo.path("avaliacoes").get(0).path("comentario").asText()).isEqualTo("Mudei de ideia");
    }

    @Test
    void notaForaDoIntervaloOuComentarioLongoSaoRecusados() {
        Livro livro = criarLivro(30, 1, 0);
        Usuario estudante = criarEstudante();

        assertThat(avaliar(estudante, livro, 0, null).status()).isEqualTo(400);
        assertThat(avaliar(estudante, livro, 6, null).status()).isEqualTo(400);
        assertThat(avaliar(estudante, livro, 4, "a".repeat(501)).status()).isEqualTo(400);
        assertThat(avaliar(estudante, livro, 4, "a".repeat(500)).status()).isEqualTo(200);
    }

    @Test
    void estudanteRemoveAPropriaAvaliacaoMasNaoADeOutro() {
        Livro livro = criarLivro(30, 1, 0);
        Usuario autor = criarEstudante();
        Usuario outro = criarEstudante();
        String avaliacaoId = avaliar(autor, livro, 2, "Fraco").corpo().path("id").asText();

        assertThat(delete("/avaliacoes/livros/" + avaliacaoId, token(outro)).status()).isEqualTo(400);
        assertThat(resumo(livro).path("total").asInt()).isEqualTo(1);

        assertThat(delete("/avaliacoes/livros/" + avaliacaoId, token(autor)).status()).isEqualTo(204);
        assertThat(resumo(livro).path("total").asInt()).isZero();
    }

    @Test
    void funcionarioNaoAvaliaEVisitanteLeAsAvaliacoes() {
        Livro livro = criarLivro(30, 1, 0);

        assertThat(avaliar(criarFuncionario(), livro, 5, null).status()).isEqualTo(403);
        assertThat(get("/avaliacoes/livro/" + livro.getId(), null).status()).isEqualTo(200);
    }

    @Test
    void rankingConsideraSomenteLivrosComTresAvaliacoes() {
        Livro comDuas = criarLivro(30, 1, 0);
        Livro comTres = criarLivro(30, 1, 0);
        avaliar(criarEstudante(), comDuas, 5, null);
        avaliar(criarEstudante(), comDuas, 5, null);
        for (int i = 0; i < 3; i++) {
            avaliar(criarEstudante(), comTres, 5, null);
        }

        assertThat(rankingContem(comDuas)).isFalse();
        assertThat(rankingContem(comTres)).isTrue();
    }
}
