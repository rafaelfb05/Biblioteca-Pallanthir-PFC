package br.com.pfc.biblioteca.service;

public interface IConverteDados {
    <T> T obterDados (String json, Class<T> classe);
}
