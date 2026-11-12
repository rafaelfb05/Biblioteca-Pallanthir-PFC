package br.com.pfc.biblioteca.service;

import br.com.pfc.biblioteca.infra.exception.RegraDeNegocioException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ConsumoApi {
    public String obterDados(String endereco) {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endereco))
                .timeout(Duration.ofSeconds(10))
                .build();
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new RegraDeNegocioException("Não foi possível conectar à API do Google Books. Tente novamente em alguns instantes.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RegraDeNegocioException("A consulta à API do Google Books foi interrompida. Tente novamente.");
        }

        if (response.statusCode() == 429) {
            throw new RegraDeNegocioException("O limite de consultas da API do Google Books foi atingido. Tente novamente mais tarde.");
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RegraDeNegocioException("A API do Google Books recusou a consulta (código " + response.statusCode() + "). Verifique a chave BOOK_API.");
        }
        return response.body();
    }
}
