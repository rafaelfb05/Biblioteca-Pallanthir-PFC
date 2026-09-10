package br.com.pfc.biblioteca.dto;

import java.util.List;

public record AtualizarLivroRequest(String titulo,
                                    List<String> autores,
                                    String anoLancamento,
                                    Integer numeroPagina,
                                    Double avaliacao) {
}
