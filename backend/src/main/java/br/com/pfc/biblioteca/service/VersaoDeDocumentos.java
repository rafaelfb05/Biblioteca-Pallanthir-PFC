package br.com.pfc.biblioteca.service;

import java.util.Arrays;
import java.util.stream.Collectors;

public final class VersaoDeDocumentos {

    private VersaoDeDocumentos() {}

    public static String calcular(String... textos) {
        String conteudo = Arrays.stream(textos)
                .map(texto -> String.valueOf(texto).replace("\r\n", "\n").strip())
                .collect(Collectors.joining("\n\u0000\n"));
        return resumo(conteudo, 1) + resumo(conteudo, 2);
    }

    private static String resumo(String texto, int semente) {
        int h1 = 0xdeadbeef ^ semente;
        int h2 = 0x41c6ce57 ^ semente;
        for (int i = 0; i < texto.length(); i++) {
            int codigo = texto.charAt(i);
            h1 = (h1 ^ codigo) * (int) 2654435761L;
            h2 = (h2 ^ codigo) * 1597334677;
        }
        h1 = ((h1 ^ (h1 >>> 16)) * (int) 2246822507L) ^ ((h2 ^ (h2 >>> 13)) * (int) 3266489909L);
        h2 = ((h2 ^ (h2 >>> 16)) * (int) 2246822507L) ^ ((h1 ^ (h1 >>> 13)) * (int) 3266489909L);
        long valor = 4294967296L * (2097151 & h2) + Integer.toUnsignedLong(h1);
        String hexa = Long.toHexString(valor);
        return "0".repeat(Math.max(0, 14 - hexa.length())) + hexa;
    }
}
