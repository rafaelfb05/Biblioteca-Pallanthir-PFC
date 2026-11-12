package br.com.pfc.biblioteca.service;

import br.com.pfc.biblioteca.infra.exception.RegraDeNegocioException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsumoApiTest {

    private HttpServer servidor;
    private final ConsumoApi consumo = new ConsumoApi();

    @BeforeEach
    void iniciarServidor() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        responder("/ok", 200, "{\"items\":[]}");
        responder("/limite", 429, "{\"error\":{\"code\":429}}");
        responder("/chave", 400, "{\"error\":{\"code\":400}}");
        servidor.start();
    }

    @AfterEach
    void pararServidor() {
        servidor.stop(0);
    }

    private void responder(String caminho, int status, String corpo) {
        servidor.createContext(caminho, troca -> {
            byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
            troca.sendResponseHeaders(status, bytes.length);
            try (OutputStream saida = troca.getResponseBody()) {
                saida.write(bytes);
            }
        });
    }

    private String endereco(String caminho) {
        return "http://localhost:" + servidor.getAddress().getPort() + caminho;
    }

    @Test
    void devolveOCorpoQuandoAApiResponde200() {
        assertThat(consumo.obterDados(endereco("/ok"))).isEqualTo("{\"items\":[]}");
    }

    @Test
    void limiteDeConsultasViraMensagemClara() {
        assertThatThrownBy(() -> consumo.obterDados(endereco("/limite")))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("limite de consultas");
    }

    @Test
    void erroDaApiIndicaAChave() {
        assertThatThrownBy(() -> consumo.obterDados(endereco("/chave")))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("BOOK_API");
    }

    @Test
    void falhaDeConexaoViraMensagemClara() throws IOException {
        int portaLivre;
        try (ServerSocket socket = new ServerSocket(0)) {
            portaLivre = socket.getLocalPort();
        }

        assertThatThrownBy(() -> consumo.obterDados("http://localhost:" + portaLivre + "/ok"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("conectar");
    }
}
