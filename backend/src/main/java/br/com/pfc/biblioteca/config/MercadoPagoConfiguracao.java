package br.com.pfc.biblioteca.config;

import com.mercadopago.MercadoPagoConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
public class MercadoPagoConfiguracao {

    @Value("${mercadopago.acess-token}")
    private String acessToken;

    @PostConstruct
    public void iniciar(){
        MercadoPagoConfig.setAccessToken(acessToken);
    }
}
