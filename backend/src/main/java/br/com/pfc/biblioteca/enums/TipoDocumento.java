package br.com.pfc.biblioteca.enums;

public enum TipoDocumento {
    TERMOS_DE_USO("Termos de Uso"),
    POLITICA_DE_PRIVACIDADE("Política de Privacidade"),
    POLITICA_DE_RESERVAS("Política de Uso de Reservas");

    private final String titulo;

    TipoDocumento(String titulo) {
        this.titulo = titulo;
    }

    public String getTitulo() {
        return titulo;
    }
}
