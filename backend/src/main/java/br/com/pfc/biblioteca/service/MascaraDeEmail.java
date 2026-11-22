package br.com.pfc.biblioteca.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MascaraDeEmail {

    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");

    private MascaraDeEmail() {}

    public static String mascarar(String email) {
        if (email == null || email.isBlank()) {
            return email;
        }
        int arroba = email.indexOf('@');
        if (arroba < 1) {
            return "•••";
        }
        String usuario = email.substring(0, arroba);
        int visiveis = usuario.length() <= 2 ? 1 : 2;
        return usuario.substring(0, visiveis) + "•••" + email.substring(arroba);
    }

    public static String mascararNoTexto(String texto) {
        if (texto == null) {
            return null;
        }
        return EMAIL.matcher(texto).replaceAll(encontrado -> Matcher.quoteReplacement(mascarar(encontrado.group())));
    }

    public static List<String> chavesDeBusca(String... textos) {
        Set<String> chaves = new LinkedHashSet<>();
        for (String texto : textos) {
            if (texto == null) {
                continue;
            }
            Matcher encontrado = EMAIL.matcher(texto);
            while (encontrado.find()) {
                chaves.add(VersaoDeDocumentos.calcular(encontrado.group().trim().toLowerCase(Locale.ROOT)));
            }
        }
        return List.copyOf(chaves);
    }

    public static boolean contemEmail(String texto) {
        return texto != null && EMAIL.matcher(texto).find();
    }
}
