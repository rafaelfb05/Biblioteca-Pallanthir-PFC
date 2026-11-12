package br.com.pfc.biblioteca.service;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MercadoPagoServiceTest {

    @Test
    void codigoExternoDoItemCabeNoLimiteDoMercadoPagoEIdentificaOLivro() {
        UUID id = UUID.randomUUID();

        String codigo = MercadoPagoService.codigoExterno(id);

        assertThat(codigo).hasSizeLessThanOrEqualTo(30).matches("[A-Za-z0-9_-]+");
        ByteBuffer bytes = ByteBuffer.wrap(Base64.getUrlDecoder().decode(codigo));
        assertThat(new UUID(bytes.getLong(), bytes.getLong())).isEqualTo(id);
    }
}
