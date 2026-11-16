package br.com.pfc.biblioteca.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VersaoDeDocumentosTest {

    @Test
    void calculaAMesmaVersaoQueOFrontend() {
        assertThat(VersaoDeDocumentos.calcular("Termos", "Privacidade")).isEqualTo("0dbdfba771f86e1677802c7812f2");
        assertThat(VersaoDeDocumentos.calcular("# Política\r\n\r\nTexto com acentuação: ção, é, ü — 😀  "))
                .isEqualTo("14f5f96593181c07310554994293");
        assertThat(VersaoDeDocumentos.calcular("")).isEqualTo("1c4425d7d009e10791d3587ca4bf");
    }

    @Test
    void ignoraQuebraDeLinhaDoWindowsEEspacosNasPontas() {
        assertThat(VersaoDeDocumentos.calcular("Texto\r\nnovo  ")).isEqualTo(VersaoDeDocumentos.calcular("Texto\nnovo"));
        assertThat(VersaoDeDocumentos.calcular("A", "B")).isNotEqualTo(VersaoDeDocumentos.calcular("A", "C"));
    }
}
