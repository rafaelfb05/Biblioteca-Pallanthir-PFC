package br.com.pfc.biblioteca.enums;

public enum TipoEstoque {
    RESERVA("para reserva"),
    COMPRA("à venda");

    private final String descricao;

    TipoEstoque(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
