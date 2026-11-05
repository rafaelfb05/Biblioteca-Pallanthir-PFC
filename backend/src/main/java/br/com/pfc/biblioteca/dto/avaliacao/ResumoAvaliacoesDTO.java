package br.com.pfc.biblioteca.dto.avaliacao;

import java.util.List;

public record ResumoAvaliacoesDTO(Double media, long total, List<AvaliacaoDTO> avaliacoes) {
}
