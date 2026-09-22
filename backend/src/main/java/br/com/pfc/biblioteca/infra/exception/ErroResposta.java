package br.com.pfc.biblioteca.infra.exception;

import java.time.LocalDateTime;

public record ErroResposta(LocalDateTime timestamp,
                           int status,
                           String erro,
                           String mensagem) {

    public ErroResposta(int status, String erro, String mensagem){
        this(LocalDateTime.now(), status, erro, mensagem);
    }
}
