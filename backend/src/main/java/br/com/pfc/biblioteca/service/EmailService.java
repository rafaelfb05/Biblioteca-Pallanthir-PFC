package br.com.pfc.biblioteca.service;

import br.com.pfc.biblioteca.infra.exception.RegraDeNegocioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private static final String TEMPLATE_CODIGO = """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <meta name="color-scheme" content="dark">
              <meta name="supported-color-schemes" content="dark">
            </head>
            <body style="margin:0;padding:0;background-color:#09090b;font-family:Arial,Helvetica,sans-serif;color:#ffffff;">
              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background-color:#09090b;">
                <tr>
                  <td align="center" style="padding:40px 16px;">
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="max-width:480px;">
                      <tr>
                        <td style="padding:0 4px 22px 4px;font-size:29px;font-weight:800;letter-spacing:-1.5px;color:#e51d2a;">Pallanthir</td>
                      </tr>
                      <tr>
                        <td style="background-color:#121216;border:1px solid #2f2f36;border-radius:12px;padding:32px 28px;">
                          <p style="margin:0 0 8px 0;font-size:11px;letter-spacing:1px;text-transform:uppercase;color:#77777f;">Biblioteca Pallanthir</p>
                          <h1 style="margin:0 0 14px 0;font-size:22px;color:#ffffff;">%s</h1>
                          <p style="margin:0 0 26px 0;font-size:14px;line-height:1.6;color:#96969d;">%s</p>
                          <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0">
                            <tr>
                              <td align="center" style="background-color:#09090b;border:1px dashed #e51d2a;border-radius:10px;padding:18px 10px;font-family:'Courier New',monospace;font-size:34px;font-weight:700;letter-spacing:8px;color:#ffffff;">%s</td>
                            </tr>
                          </table>
                          <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="margin-top:18px;">
                            <tr>
                              <td align="center" style="border-radius:8px;background-color:#e51d2a;">
                                <a href="%s" target="_blank" style="display:block;padding:13px 20px;font-size:14px;font-weight:700;color:#ffffff;text-decoration:none;">Copiar código</a>
                              </td>
                            </tr>
                          </table>
                          <p style="margin:22px 0 0 0;font-size:13px;line-height:1.5;color:#96969d;">O código vale por <strong style="color:#ffffff;">%d minutos</strong>.</p>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:22px 4px 0 4px;font-size:12px;line-height:1.5;color:#77777f;">
                          Se você não fez essa solicitação, ignore este e-mail. Nunca compartilhe este código com outras pessoas.
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """;

    private final RestClient restClient;
    private final String apiKey;
    private final String remetente;
    private final String frontendUrl;

    public EmailService(@Value("${brevo.api-key}") String apiKey,
                        @Value("${brevo.remetente}") String remetente,
                        @Value("${app.frontend-url}") String frontendUrl) {
        this.apiKey = apiKey;
        this.remetente = remetente;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        this.restClient = RestClient.builder()
                .baseUrl("https://api.brevo.com/v3")
                .requestFactory(requestFactory)
                .build();
    }

    public void enviarCodigo(String destinatario, String assunto, String titulo, String mensagem, String codigo, int minutosValidade){
        String linkCopiar = frontendUrl + "/?copiar=" + codigo;

        String texto = titulo + "\n\n" + mensagem + "\n\nSeu código: " + codigo +
                "\nO código vale por " + minutosValidade + " minutos." +
                "\n\nPara copiar o código, acesse: " + linkCopiar +
                "\n\nSe você não fez essa solicitação, ignore este e-mail.";

        String html = TEMPLATE_CODIGO.formatted(titulo, mensagem, codigo, linkCopiar, minutosValidade);

        enviar(destinatario, assunto, texto, html);
    }

    private void enviar(String destinatario, String assunto, String texto, String html){
        Map<String, Object> mensagem = Map.of(
                "sender", Map.of("name", "Biblioteca Pallanthir", "email", remetente),
                "to", List.of(Map.of("email", destinatario)),
                "subject", assunto,
                "textContent", texto,
                "htmlContent", html
        );

        try {
            restClient.post()
                    .uri("/smtp/email")
                    .header("api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(mensagem)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            logger.error("Falha ao enviar email para {}: {}", destinatario, e.getMessage());
            throw new RegraDeNegocioException("Não foi possível enviar o email agora. Tente novamente em instantes.");
        }
    }
}
