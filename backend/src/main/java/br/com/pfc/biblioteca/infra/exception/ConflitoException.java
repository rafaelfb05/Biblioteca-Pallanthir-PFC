package br.com.pfc.biblioteca.infra.exception;

public class ConflitoException extends RuntimeException{
    public ConflitoException(String mensagem){
        super(mensagem);
    }
}
