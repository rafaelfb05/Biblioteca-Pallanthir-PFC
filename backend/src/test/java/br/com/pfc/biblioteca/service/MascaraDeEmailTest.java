package br.com.pfc.biblioteca.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MascaraDeEmailTest {

    @Test
    void escondeOEmailDeixandoSoAsPrimeirasLetrasEODominio() {
        assertThat(MascaraDeEmail.mascarar("felipe.miranda@gmail.com")).isEqualTo("fe•••@gmail.com");
        assertThat(MascaraDeEmail.mascarar("ab@teste.com")).isEqualTo("a•••@teste.com");
        assertThat(MascaraDeEmail.mascarar(null)).isNull();
    }

    @Test
    void escondeOsEmailsQueAparecemNoMeioDoTexto() {
        assertThat(MascaraDeEmail.mascararNoTexto("Perfil de ana@teste.com alterado por bruno.silva@escola.edu.br"))
                .isEqualTo("Perfil de an•••@teste.com alterado por br•••@escola.edu.br");
        assertThat(MascaraDeEmail.contemEmail("Login concluído")).isFalse();
    }

    @Test
    void chaveDeBuscaIgnoraMaiusculasERepeticoes() {
        assertThat(MascaraDeEmail.chavesDeBusca("Ana@Teste.com", "Login de ana@teste.com"))
                .containsExactly(VersaoDeDocumentos.calcular("ana@teste.com"));
        assertThat(MascaraDeEmail.chavesDeBusca(null, "sem e-mail")).isEmpty();
    }
}
