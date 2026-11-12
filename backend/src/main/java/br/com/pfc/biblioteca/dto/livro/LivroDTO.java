package br.com.pfc.biblioteca.dto.livro;

import java.util.UUID;
import br.com.pfc.biblioteca.enums.Materia;
import br.com.pfc.biblioteca.entity.jpa.Livro;

import java.util.List;

public record LivroDTO(UUID id,
                       String titulo,
                       List<String> autores,
                       String anoLancamento,
                       int numeroPagina,
                       String capaUrl,
                       Materia materia,
                       double preco,
                       Integer estoqueReserva,
                       Integer estoqueCompra) {

    public LivroDTO(Livro livro) {
        this(livro.getId(), livro.getTitulo(),
                livro.getAutores(), livro.getAnoLancamento(),
                livro.getNumeroPagina(), livro.getCapaUrl(),
                livro.getMateria(), livro.getPreco(),
                livro.getEstoqueReserva(), livro.getEstoqueCompra());
    }
}
