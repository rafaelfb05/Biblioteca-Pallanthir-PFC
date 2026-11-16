package br.com.pfc.biblioteca.service;

import br.com.pfc.biblioteca.entity.jpa.Livro;
import br.com.pfc.biblioteca.enums.TipoEstoque;
import br.com.pfc.biblioteca.infra.exception.RegraDeNegocioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.HtmlUtils;

import java.net.http.HttpClient;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
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
                              <td align="center" style="background-color:#09090b;border:1px dashed #e51d2a;border-radius:10px;padding:18px 10px;font-family:'Courier New',monospace;font-size:34px;font-weight:700;letter-spacing:8px;color:#ffffff;-webkit-user-select:all;user-select:all;">%s</td>
                            </tr>
                          </table>
                          <p style="margin:12px 0 0 0;font-size:12px;line-height:1.5;text-align:center;color:#77777f;">Para copiar, dê dois cliques no código (computador) ou toque e segure sobre ele (celular).</p>
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

    private static final String TEMPLATE_DISPONIBILIDADE = """
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
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="max-width:520px;">
                      <tr>
                        <td style="padding:0 4px 22px 4px;font-size:29px;font-weight:800;letter-spacing:-1.5px;color:#e51d2a;">Pallanthir</td>
                      </tr>
                      <tr>
                        <td style="background-color:#121216;border:1px solid #2f2f36;border-radius:14px;padding:32px 28px;">
                          <table role="presentation" cellpadding="0" cellspacing="0" border="0">
                            <tr>
                              <td style="background-color:#13261a;border:1px solid #2f6b3d;border-radius:20px;padding:6px 12px;font-size:11px;font-weight:700;letter-spacing:1px;text-transform:uppercase;color:#7ee08f;">&#128276; De volta ao estoque</td>
                            </tr>
                          </table>
                          <h1 style="margin:18px 0 10px 0;font-size:24px;line-height:1.3;color:#ffffff;">Boa notícia, %s!</h1>
                          <p style="margin:0 0 24px 0;font-size:15px;line-height:1.6;color:#b4b4bb;">O livro que você pediu para acompanhar voltou a ter exemplares <strong style="color:#ffffff;">%s</strong>.</p>
                          <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background-color:#0d0d10;border:1px solid #26262d;border-radius:12px;">
                            <tr>
                              <td width="132" valign="top" style="padding:18px 0 18px 18px;">%s</td>
                              <td valign="top" style="padding:18px;">
                                <p style="margin:0 0 6px 0;font-size:18px;font-weight:700;line-height:1.35;color:#ffffff;">%s</p>
                                <p style="margin:0 0 14px 0;font-size:13px;line-height:1.5;color:#96969d;">%s</p>
                                <table role="presentation" cellpadding="0" cellspacing="0" border="0">
                                  <tr>
                                    <td style="background-color:#2a1215;border-radius:20px;padding:6px 12px;font-size:12px;font-weight:700;color:#ff5560;">%s</td>
                                  </tr>
                                </table>
                              </td>
                            </tr>
                          </table>
                          <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="margin-top:26px;">
                            <tr>
                              <td align="center">
                                <a href="%s" style="display:inline-block;background-color:#e51d2a;border-radius:10px;padding:15px 34px;font-size:15px;font-weight:700;color:#ffffff;text-decoration:none;">Ver o livro</a>
                              </td>
                            </tr>
                          </table>
                          <p style="margin:24px 0 0 0;font-size:13px;line-height:1.6;color:#96969d;text-align:center;">Os exemplares são limitados e ficam com quem %s primeiro.</p>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:22px 4px 0 4px;font-size:12px;line-height:1.5;color:#77777f;">
                          Você recebeu este e-mail porque pediu para ser avisado quando este livro tivesse estoque. O aviso é enviado uma única vez.
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """;

    private static final String CAPA_COM_IMAGEM =
            "<img src=\"%s\" width=\"114\" alt=\"Capa do livro\" style=\"display:block;width:114px;max-width:114px;height:auto;border-radius:6px;border:1px solid #26262d;\">";

    private static final String CAPA_GENERICA =
            "<table role=\"presentation\" width=\"114\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr>"
                    + "<td align=\"center\" valign=\"middle\" height=\"168\" style=\"background-color:#f4f1ea;border-radius:6px;padding:10px;"
                    + "font-family:Georgia,serif;font-size:14px;font-weight:700;line-height:1.3;color:#1a1a1a;\">%s"
                    + "<span style=\"display:block;margin-top:12px;font-family:Arial,Helvetica,sans-serif;font-size:9px;letter-spacing:1px;color:#8a8a93;\">PALLANTHIR</span></td>"
                    + "</tr></table>";

    @Value("${app.frontend-url}")
    private String frontendUrl;

    private final RestClient restClient;
    private final String apiKey;
    private final String remetente;

    public EmailService(@Value("${brevo.api-key}") String apiKey,
                        @Value("${brevo.remetente}") String remetente) {
        this.apiKey = apiKey;
        this.remetente = remetente;

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
        String texto = titulo + "\n\n" + mensagem + "\n\nSeu código: " + codigo +
                "\nO código vale por " + minutosValidade + " minutos." +
                "\n\nSe você não fez essa solicitação, ignore este e-mail.";

        String html = TEMPLATE_CODIGO.formatted(titulo, mensagem, codigo, minutosValidade);

        enviar(destinatario, assunto, texto, html);
    }

    public void enviarAvisoDisponibilidade(String destinatario, String nome, Livro livro, TipoEstoque tipo) {
        String titulo = livro.getTitulo();
        String autores = livro.getAutores() == null || livro.getAutores().isEmpty()
                ? "Autor desconhecido"
                : String.join(", ", livro.getAutores());
        String primeiroNome = nome == null || nome.isBlank() ? "leitor" : nome.trim().split("\\s+")[0];
        String disponibilidade = tipo == TipoEstoque.RESERVA
                ? "Disponível para reserva"
                : "À venda por " + NumberFormat.getCurrencyInstance(Locale.of("pt", "BR")).format(livro.getPreco());
        String link = frontendUrl.replaceAll("/+$", "") + "/?livro=" + livro.getId();
        String capaUrl = livro.getCapaUrl();
        String capa = capaUrl != null && capaUrl.startsWith("https://")
                ? CAPA_COM_IMAGEM.formatted(HtmlUtils.htmlEscape(capaUrl))
                : CAPA_GENERICA.formatted(HtmlUtils.htmlEscape(titulo));

        String html = TEMPLATE_DISPONIBILIDADE.formatted(
                HtmlUtils.htmlEscape(primeiroNome),
                tipo.getDescricao(),
                capa,
                HtmlUtils.htmlEscape(titulo),
                HtmlUtils.htmlEscape(autores),
                HtmlUtils.htmlEscape(disponibilidade),
                HtmlUtils.htmlEscape(link),
                tipo == TipoEstoque.RESERVA ? "reservar" : "comprar");

        String texto = "Boa notícia, " + primeiroNome + "!\n\n"
                + "O livro \"" + titulo + "\" voltou a ter exemplares " + tipo.getDescricao() + ".\n"
                + disponibilidade + ".\n\nVeja o livro: " + link
                + "\n\nVocê recebeu este e-mail porque pediu para ser avisado quando este livro tivesse estoque.";

        enviar(destinatario, "\"" + titulo + "\" voltou ao estoque - Biblioteca Pallanthir", texto, html);
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
