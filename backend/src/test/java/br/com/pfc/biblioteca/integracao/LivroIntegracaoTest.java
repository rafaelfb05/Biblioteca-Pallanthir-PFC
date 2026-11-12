package br.com.pfc.biblioteca.integracao;

import br.com.pfc.biblioteca.entity.jpa.Livro;
import br.com.pfc.biblioteca.entity.jpa.Usuario;
import br.com.pfc.biblioteca.service.ConsumoApi;
import br.com.pfc.biblioteca.service.LivroService;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LivroIntegracaoTest extends TesteIntegracaoBase {

    @Autowired
    private LivroService livroService;

    private Object consumoOriginal;
    private final List<String> buscasNaGoogleBooks = new ArrayList<>();

    @BeforeEach
    void simularGoogleBooks() {
        Object alvo = AopTestUtils.getUltimateTargetObject(livroService);
        consumoOriginal = ReflectionTestUtils.getField(alvo, "consumo");
        ReflectionTestUtils.setField(alvo, "consumo", new ConsumoApi() {
            @Override
            public String obterDados(String endereco) {
                String consulta = URI.create(endereco).getRawQuery().split("&")[0].substring(2);
                String titulo = URLDecoder.decode(consulta, StandardCharsets.UTF_8);
                buscasNaGoogleBooks.add(titulo);
                if (titulo.startsWith("Inexistente")) {
                    return "{\"totalItems\":0}";
                }
                return "{\"items\":[{\"volumeInfo\":{\"title\":" + texto(titulo) + ",\"authors\":[\"J. R. R. Tolkien\"],"
                        + "\"publishedDate\":\"1937\",\"pageCount\":310,"
                        + "\"imageLinks\":{\"thumbnail\":\"http://books.google.com/capa.jpg\"}}}]}";
            }
        });
    }

    @AfterEach
    void restaurarGoogleBooks() {
        Object alvo = AopTestUtils.getUltimateTargetObject(livroService);
        ReflectionTestUtils.setField(alvo, "consumo", consumoOriginal);
    }

    private String texto(String valor) {
        try {
            return json.writeValueAsString(valor);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private Map<String, Object> novoLivro(String titulo) {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("titulo", titulo);
        corpo.put("materia", "DIREITO");
        corpo.put("preco", 50.0);
        corpo.put("estoqueReserva", 2);
        corpo.put("estoqueCompra", 3);
        return corpo;
    }

    private boolean catalogoContem(long livroId) {
        for (JsonNode livro : get("/livros", null).corpo()) {
            if (livro.path("id").asLong() == livroId) {
                return true;
            }
        }
        return false;
    }

    @Test
    void cadastroPreencheOsDadosPelaGoogleBooks() {
        Usuario funcionario = criarFuncionario();

        Resposta resposta = post("/livros/cadastro", token(funcionario), novoLivro("O Hobbit"));

        assertThat(resposta.status()).isEqualTo(200);
        assertThat(resposta.corpo().path("titulo").asText()).isEqualTo("O Hobbit");
        assertThat(resposta.corpo().path("autores").get(0).asText()).isEqualTo("J. R. R. Tolkien");
        assertThat(resposta.corpo().path("numeroPagina").asInt()).isEqualTo(310);
        assertThat(resposta.corpo().path("estoqueReserva").asInt()).isEqualTo(2);
        assertThat(resposta.corpo().path("estoqueCompra").asInt()).isEqualTo(3);
        assertThat(catalogoContem(resposta.corpo().path("id").asLong())).isTrue();
    }

    @Test
    void cadastroAceitaTitulosComCaracteresEspeciais() {
        Usuario funcionario = criarFuncionario();
        List<String> titulos = List.of("100% Jovem", "Direito \"Civil\"", "Algoritmos | Teoria", "C# 10 e .NET 6",
                "Física & Química: volume 1", "O Senhor dos Anéis");

        for (String titulo : titulos) {
            Resposta resposta = post("/livros/cadastro", token(funcionario), novoLivro(titulo));
            assertThat(resposta.status()).as(titulo).isEqualTo(200);
            assertThat(resposta.corpo().path("titulo").asText()).isEqualTo(titulo);
        }
        assertThat(buscasNaGoogleBooks).containsExactlyElementsOf(titulos);
    }

    @Test
    void cadastroDeLivroQueNaoExisteNaGoogleBooksDevolve404() {
        Resposta resposta = post("/livros/cadastro", token(criarFuncionario()), novoLivro("Inexistente qualquer"));

        assertThat(resposta.status()).isEqualTo(404);
    }

    @Test
    void estudanteNaoCadastraLivro() {
        Resposta resposta = post("/livros/cadastro", token(criarEstudante()), novoLivro("O Hobbit"));

        assertThat(resposta.status()).isEqualTo(403);
        assertThat(buscasNaGoogleBooks).isEmpty();
    }

    @Test
    void edicaoAlteraSomenteOsCamposEnviados() {
        Livro livro = criarLivro(50, 2, 3);

        Resposta resposta = put("/livros/atualizar/" + livro.getId(), token(criarFuncionario()), Map.of("preco", 70.0, "estoqueCompra", 8));

        assertThat(resposta.status()).isEqualTo(200);
        Livro atualizado = recarregar(livro);
        assertThat(atualizado.getPreco()).isEqualTo(70.0);
        assertThat(atualizado.getEstoqueCompra()).isEqualTo(8);
        assertThat(atualizado.getEstoqueReserva()).isEqualTo(2);
        assertThat(atualizado.getTitulo()).isEqualTo(livro.getTitulo());
    }

    @Test
    void edicaoDeLivroInexistenteDevolve404() {
        Resposta resposta = put("/livros/atualizar/999999", token(criarFuncionario()), Map.of("preco", 70.0));

        assertThat(resposta.status()).isEqualTo(404);
    }

    @Test
    void exclusaoELogicaETiraOLivroDoCatalogo() {
        Livro livro = criarLivro(50, 1, 0);

        Resposta resposta = delete("/livros/deletar/" + livro.getId(), token(criarFuncionario()));

        assertThat(resposta.status()).isEqualTo(204);
        assertThat(recarregar(livro).isAtivo()).isFalse();
        assertThat(catalogoContem(livro.getId())).isFalse();
    }

    @Test
    void exclusaoDeLivroComReservaPedeConfirmacaoERemoveDosFavoritos() {
        Usuario estudante = criarEstudante();
        Usuario funcionario = criarFuncionario();
        Livro livro = criarLivro(50, 1, 0);
        post("/usuarios/" + estudante.getId() + "/favoritar/" + livro.getId(), token(estudante), null);
        post("/reservas/" + estudante.getId() + "/" + livro.getId(), token(estudante), null);

        assertThat(delete("/livros/deletar/" + livro.getId(), token(funcionario)).status()).isEqualTo(409);
        assertThat(recarregar(livro).isAtivo()).isTrue();

        assertThat(delete("/livros/deletar/" + livro.getId() + "?confirmado=true", token(funcionario)).status()).isEqualTo(204);
        assertThat(get("/usuarios/" + estudante.getId() + "/favoritos", token(estudante)).corpo().size()).isZero();
    }

    @Test
    void catalogoEPublico() {
        Livro livro = criarLivro(50, 1, 0);

        assertThat(get("/livros", null).status()).isEqualTo(200);
        assertThat(catalogoContem(livro.getId())).isTrue();
    }

    @Test
    void estudanteFavoritaSemDuplicarEFuncionarioNaoFavorita() {
        Usuario estudante = criarEstudante();
        Livro livro = criarLivro(50, 1, 0);
        String caminho = "/usuarios/" + estudante.getId() + "/favoritar/" + livro.getId();

        post(caminho, token(estudante), null);
        post(caminho, token(estudante), null);

        assertThat(get("/usuarios/" + estudante.getId() + "/favoritos", token(estudante)).corpo().size()).isEqualTo(1);
        Usuario funcionario = criarFuncionario();
        assertThat(post("/usuarios/" + funcionario.getId() + "/favoritar/" + livro.getId(), token(funcionario), null).status()).isEqualTo(403);
    }
}
