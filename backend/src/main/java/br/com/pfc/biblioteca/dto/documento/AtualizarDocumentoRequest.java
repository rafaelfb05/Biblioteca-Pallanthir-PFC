package br.com.pfc.biblioteca.dto.documento;

import br.com.pfc.biblioteca.entity.jpa.Documento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AtualizarDocumentoRequest(
        @NotBlank(message = "O documento não pode ficar vazio")
        @Size(max = Documento.TAMANHO_MAXIMO, message = "O documento pode ter no máximo 100.000 caracteres")
        String conteudo) {
}
