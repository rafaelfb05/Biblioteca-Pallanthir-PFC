package br.com.pfc.biblioteca.infra.exception;

public class CredenciaisInvalidasException extends RuntimeException{
    public CredenciaisInvalidasException(String mensagem){
        super(mensagem);
    }
}
