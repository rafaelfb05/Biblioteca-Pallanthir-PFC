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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LivroIntegracaoTest extends TesteIntegracaoBase {

    @Autowired
    private LivroService livroService;

    private Object consumoOriginal;
    private final List<String> buscasNaGoogleBooks = new ArrayList<>();
    private final List<String> capasNaOpenLibrary = new ArrayList<>();
    private final List<String> consultasNaOpenLibrary = new ArrayList<>();
    private final List<String> buscasPorTitulo = new ArrayList<>();
    private final List<String> imagensValidas = new ArrayList<>();
    private String respostaDaBuscaPorTitulo = "{\"docs\":[]}";

    @BeforeEach
    void simularGoogleBooks() {
        livroRepository.findAll().stream()
                .filter(Livro::isAtivo)
                .filter(livro -> (livro.getIsbn13() != null && List.of("9788595084742", "9780000000001", "9781111111111").contains(livro.getIsbn13()))
                        || "8595084742".equals(livro.getIsbn10()))
                .forEach(livro -> {
                    livro.setAtivo(false);
                    livroRepository.save(livro);
                });
        Object alvo = AopTestUtils.getUltimateTargetObject(livroService);
        consumoOriginal = ReflectionTestUtils.getField(alvo, "consumo");
        ReflectionTestUtils.setField(alvo, "consumo", new ConsumoApi() {
            @Override
            public String obterDados(String endereco) {
                if (endereco.startsWith("https://openlibrary.org/search.json")) {
                    String titulo = endereco.substring(endereco.indexOf("&title=") + 7);
                    buscasPorTitulo.add(URLDecoder.decode(titulo, StandardCharsets.UTF_8));
                    return respostaDaBuscaPorTitulo;
                }
                String consulta = URI.create(endereco).getRawQuery().split("&")[0].substring(2);
                String titulo = URLDecoder.decode(consulta, StandardCharsets.UTF_8);
                buscasNaGoogleBooks.add(titulo);
                if (titulo.startsWith("Inexistente")) {
                    return "{\"totalItems\":0}";
                }
                if (titulo.startsWith("Sem capa")) {
                    return "{\"items\":[{\"volumeInfo\":{\"title\":" + texto(titulo) + ",\"pageCount\":90}}]}";
                }
                String numero = String.format("%09d", Math.abs((long) titulo.hashCode()) % 1_000_000_000L);
                String isbn10 = titulo.startsWith("O Hobbit") ? "8595084742" : numero + "0";
                String isbn13 = titulo.startsWith("O Hobbit") ? "9788595084742" : "9785" + numero;
                return "{\"items\":[{\"volumeInfo\":{\"title\":" + texto(titulo) + ",\"authors\":[\"J. R. R. Tolkien\"],"
                        + "\"publishedDate\":\"1937\",\"pageCount\":310,"
                        + "\"imageLinks\":{\"thumbnail\":\"http://books.google.com/capa.jpg\"},"
                        + "\"industryIdentifiers\":[{\"type\":\"ISBN_10\",\"identifier\":\"" + isbn10 + "\"},"
                        + "{\"type\":\"ISBN_13\",\"identifier\":\"" + isbn13 + "\"}]}}]}";
            }

            @Override
            public boolean imagemExiste(String endereco) {
                if (imagensValidas.contains(endereco)) {
                    return true;
                }
                consultasNaOpenLibrary.add(endereco);
                return capasNaOpenLibrary.stream().anyMatch(isbn -> endereco.equals("https://covers.openlibrary.org/b/isbn/" + isbn + "-L.jpg?default=false"));
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

    private boolean catalogoContem(Object livroId) {
        for (JsonNode livro : get("/livros", null).corpo()) {
            if (livro.path("id").asText().equals(livroId.toString())) {
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
        assertThat(resposta.corpo().path("isbn10").asText()).isEqualTo("8595084742");
        assertThat(resposta.corpo().path("isbn13").asText()).isEqualTo("9788595084742");
        assertThat(catalogoContem(resposta.corpo().path("id").asText())).isTrue();
    }

    @Test
    void edicaoDeIsbnAceitaHifenRecusaFormatoInvalidoEApagaQuandoVazio() {
        Livro livro = criarLivro(50, 1, 0);
        String funcionario = token(criarFuncionario());

        Resposta comHifen = put("/livros/atualizar/" + livro.getId(), funcionario, Map.of("isbn10", "85-950-8474-x", "isbn13", "978-85-950-8474-2"));
        assertThat(comHifen.status()).isEqualTo(200);
        assertThat(comHifen.corpo().path("isbn10").asText()).isEqualTo("859508474X");
        assertThat(comHifen.corpo().path("isbn13").asText()).isEqualTo("9788595084742");

        assertThat(put("/livros/atualizar/" + livro.getId(), funcionario, Map.of("isbn13", "12345")).status()).isEqualTo(400);
        assertThat(put("/livros/atualizar/" + livro.getId(), funcionario, Map.of("isbn10", "ABCDEFGHIJ")).status()).isEqualTo(400);
        assertThat(recarregar(livro).getIsbn13()).isEqualTo("9788595084742");

        Resposta vazio = put("/livros/atualizar/" + livro.getId(), funcionario, Map.of("isbn10", "", "isbn13", ""));
        assertThat(vazio.status()).isEqualTo(200);
        assertThat(recarregar(livro).getIsbn10()).isNull();
        assertThat(recarregar(livro).getIsbn13()).isNull();
    }

    @Test
    void cadastroUsaACapaDaOpenLibraryQuandoElaExiste() {
        capasNaOpenLibrary.add("9788595084742");

        Resposta resposta = post("/livros/cadastro", token(criarFuncionario()), novoLivro("O Hobbit"));

        assertThat(resposta.status()).isEqualTo(200);
        assertThat(resposta.corpo().path("capaUrl").asText()).isEqualTo("https://covers.openlibrary.org/b/isbn/9788595084742-L.jpg");
    }

    @Test
    void cadastroUsaACapaGenericaQuandoAOpenLibraryNaoTem() {
        Resposta resposta = post("/livros/cadastro", token(criarFuncionario()), novoLivro("O Hobbit"));

        assertThat(resposta.status()).isEqualTo(200);
        assertThat(resposta.corpo().path("capaUrl").isNull()).isTrue();
        assertThat(consultasNaOpenLibrary).hasSize(2);
    }

    @Test
    void cadastroSemIsbnUsaACapaGenerica() {
        Resposta resposta = post("/livros/cadastro", token(criarFuncionario()), novoLivro("Sem capa nenhuma"));

        assertThat(resposta.status()).isEqualTo(200);
        assertThat(resposta.corpo().path("capaUrl").isNull()).isTrue();
        assertThat(consultasNaOpenLibrary).isEmpty();
    }

    @Test
    void edicaoComIsbnNovoTrocaACapaPelaDaOpenLibrary() {
        Livro livro = criarLivro(50, 1, 0);
        livro.setCapaUrl("http://books.google.com/antiga.jpg");
        livroRepository.save(livro);
        capasNaOpenLibrary.add("8595084742");

        Resposta resposta = put("/livros/atualizar/" + livro.getId(), token(criarFuncionario()), Map.of("isbn13", "9780000000001", "isbn10", "85-950-8474-2"));

        assertThat(resposta.status()).isEqualTo(200);
        assertThat(resposta.corpo().path("capaUrl").asText()).isEqualTo("https://covers.openlibrary.org/b/isbn/8595084742-L.jpg");
        assertThat(recarregar(livro).getCapaUrl()).isEqualTo("https://covers.openlibrary.org/b/isbn/8595084742-L.jpg");
    }

    @Test
    void edicaoComIsbnSemCapaNaOpenLibraryColocaACapaGenerica() {
        Livro livro = criarLivro(50, 1, 0);
        livro.setCapaUrl("http://books.google.com/antiga.jpg");
        livroRepository.save(livro);

        Resposta resposta = put("/livros/atualizar/" + livro.getId(), token(criarFuncionario()), Map.of("isbn13", "9780000000001"));

        assertThat(resposta.status()).isEqualTo(200);
        assertThat(recarregar(livro).getCapaUrl()).isNull();
    }

    @Test
    void edicaoSemMudarOIsbnMantemACapa() {
        Livro livro = criarLivro(50, 1, 0);
        livro.setCapaUrl("http://books.google.com/antiga.jpg");
        livro.setIsbn13("9780000000001");
        livroRepository.save(livro);

        Resposta resposta = put("/livros/atualizar/" + livro.getId(), token(criarFuncionario()), Map.of("preco", 60.0, "isbn13", "978-0000000001", "isbn10", ""));

        assertThat(resposta.status()).isEqualTo(200);
        assertThat(recarregar(livro).getCapaUrl()).isEqualTo("http://books.google.com/antiga.jpg");
        assertThat(consultasNaOpenLibrary).isEmpty();
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
        Resposta resposta = put("/livros/atualizar/" + UUID.randomUUID(), token(criarFuncionario()), Map.of("preco", 70.0));

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

    @Test
    void semCapaPeloIsbnBuscaPeloTituloEConfereOAutor() {
        respostaDaBuscaPorTitulo = "{\"docs\":["
                + "{\"title\":\"O Hobbit\",\"author_name\":[\"Fulano de Tal\"],\"cover_i\":111},"
                + "{\"title\":\"O Hobbit Ilustrado\",\"author_name\":[\"J. R. R. Tolkien\"],\"cover_i\":222},"
                + "{\"title\":\"Hobbit, O\",\"author_name\":[\"J.R.R. Tolkien\"]},"
                + "{\"title\":\"Hobbit, O\",\"author_name\":[\"John Ronald Reuel Tolkien\"],\"cover_i\":333}]}";

        Resposta resposta = post("/livros/cadastro", token(criarFuncionario()), novoLivro("O Hobbit"));

        assertThat(resposta.status()).isEqualTo(200);
        assertThat(resposta.corpo().path("capaUrl").asText()).isEqualTo("https://covers.openlibrary.org/b/id/333-L.jpg");
        assertThat(buscasPorTitulo).containsExactly("O Hobbit");
    }

    @Test
    void semCapaPeloIsbnNemPeloTituloFicaComACapaGenerica() {
        respostaDaBuscaPorTitulo = "{\"docs\":[{\"title\":\"Outro livro qualquer\",\"author_name\":[\"J. R. R. Tolkien\"],\"cover_i\":444}]}";

        Resposta resposta = post("/livros/cadastro", token(criarFuncionario()), novoLivro("O Hobbit"));

        assertThat(resposta.status()).isEqualTo(200);
        assertThat(resposta.corpo().path("capaUrl").isNull()).isTrue();
    }

    @Test
    void capaComIsbnNaoPrecisaDaBuscaPorTitulo() {
        capasNaOpenLibrary.add("9788595084742");

        post("/livros/cadastro", token(criarFuncionario()), novoLivro("O Hobbit"));

        assertThat(buscasPorTitulo).isEmpty();
    }

    @Test
    void funcionarioColaOLinkDaCapaNaEdicao() {
        Livro livro = criarLivro(50, 1, 0);
        String funcionario = token(criarFuncionario());
        String link = "https://m.media-amazon.com/images/I/capa-teste.jpg";
        imagensValidas.add(link);

        Resposta salvo = put("/livros/atualizar/" + livro.getId(), funcionario, Map.of("capaUrl", "  " + link + " "));
        assertThat(salvo.status()).isEqualTo(200);
        assertThat(salvo.corpo().path("capaUrl").asText()).isEqualTo(link);
        assertThat(recarregar(livro).getCapaUrl()).isEqualTo(link);

        Resposta semHttps = put("/livros/atualizar/" + livro.getId(), funcionario, Map.of("capaUrl", "http://exemplo.com/capa.jpg"));
        assertThat(semHttps.status()).isEqualTo(400);
        assertThat(semHttps.mensagem()).contains("https://");

        Resposta naoImagem = put("/livros/atualizar/" + livro.getId(), funcionario, Map.of("capaUrl", "https://exemplo.com/pagina-do-livro"));
        assertThat(naoImagem.status()).isEqualTo(400);
        assertThat(naoImagem.mensagem()).contains("Copiar endereço da imagem");
        assertThat(recarregar(livro).getCapaUrl()).isEqualTo(link);

        Resposta outroCampo = put("/livros/atualizar/" + livro.getId(), funcionario, Map.of("preco", 60));
        assertThat(outroCampo.status()).isEqualTo(200);
        assertThat(recarregar(livro).getCapaUrl()).isEqualTo(link);

        Resposta automatica = put("/livros/atualizar/" + livro.getId(), funcionario, Map.of("capaUrl", ""));
        assertThat(automatica.status()).isEqualTo(200);
        assertThat(recarregar(livro).getCapaUrl()).isNull();
    }

    @Test
    void naoCadastraOMesmoLivroDuasVezes() {
        String funcionario = token(criarFuncionario());

        Resposta primeiro = post("/livros/cadastro", funcionario, novoLivro("O Hobbit"));
        assertThat(primeiro.status()).isEqualTo(200);

        Resposta repetido = post("/livros/cadastro", funcionario, novoLivro("O Hobbit edição especial"));
        assertThat(repetido.status()).isEqualTo(409);
        assertThat(repetido.mensagem()).contains("já está no acervo");

        put("/livros/atualizar/" + primeiro.corpo().path("id").asText(), funcionario, Map.of("isbn10", "", "isbn13", ""));
        assertThat(post("/livros/cadastro", funcionario, novoLivro("O Hobbit edição especial")).status()).isEqualTo(200);
    }

    @Test
    void livroSemIsbnRepetidoPeloTituloEAutorTambemEBloqueado() {
        String funcionario = token(criarFuncionario());
        String codigo = UUID.randomUUID().toString().substring(0, 8);

        assertThat(post("/livros/cadastro", funcionario, novoLivro("Sem capa " + codigo)).status()).isEqualTo(200);
        assertThat(post("/livros/cadastro", funcionario, novoLivro("Sem capa: " + codigo.toUpperCase() + "!")).status()).isEqualTo(409);
    }

    @Test
    void edicaoNaoUsaOIsbnDeOutroLivro() {
        String funcionario = token(criarFuncionario());
        Livro primeiro = criarLivro(10, 1, 0);
        primeiro.setIsbn13("9781111111111");
        livroRepository.save(primeiro);
        Livro segundo = criarLivro(10, 1, 0);

        Resposta resposta = put("/livros/atualizar/" + segundo.getId(), funcionario, Map.of("isbn13", "978-1111111111"));

        assertThat(resposta.status()).isEqualTo(409);
        assertThat(resposta.mensagem()).contains(primeiro.getTitulo());
        assertThat(recarregar(segundo).getIsbn13()).isNull();
    }
}
