package br.com.pfc.biblioteca.dto;

import java.util.List;

public record ResumoAvaliacoesDTO(Double media, long total, List<AvaliacaoDTO> avaliacoes) {
}
