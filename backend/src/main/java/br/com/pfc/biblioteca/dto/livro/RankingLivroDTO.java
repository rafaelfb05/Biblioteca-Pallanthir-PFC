package br.com.pfc.biblioteca.dto.livro;

import java.util.UUID;
public record RankingLivroDTO(UUID id, String titulo, String capaUrl, Double media, Long total) {
}
