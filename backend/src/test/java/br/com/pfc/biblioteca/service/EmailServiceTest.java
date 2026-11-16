package br.com.pfc.biblioteca.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailServiceTest {

    @Test
    void capaAntigaDoGoogleBooksViraHttpsNoEmail() {
        assertThat(EmailService.capaParaEmail("http://books.google.com/books/content?id=abc&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api"))
                .isEqualTo("https://books.google.com/books/content?id=abc&printsec=frontcover&img=1&zoom=1&source=gbs_api");
    }

    @Test
    void capaHttpsContinuaIgualECapaInvalidaFicaGenerica() {
        assertThat(EmailService.capaParaEmail("https://covers.openlibrary.org/b/id/1-L.jpg"))
                .isEqualTo("https://covers.openlibrary.org/b/id/1-L.jpg");
        assertThat(EmailService.capaParaEmail(null)).isNull();
        assertThat(EmailService.capaParaEmail("  ")).isNull();
        assertThat(EmailService.capaParaEmail("ftp://exemplo.com/capa.jpg")).isNull();
    }
}
