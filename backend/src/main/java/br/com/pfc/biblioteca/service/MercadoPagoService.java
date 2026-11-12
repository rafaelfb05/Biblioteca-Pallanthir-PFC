package br.com.pfc.biblioteca.service;

import br.com.pfc.biblioteca.entity.jpa.ItemPedido;
import br.com.pfc.biblioteca.entity.jpa.Pedido;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class MercadoPagoService {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoService.class);
    private static final String API = "https://api.mercadopago.com/v1/orders";

    public record OrderCriada(String id, String checkoutUrl){}
    public record OrderMP(String id, String externalReference, String status, String statusDetail){}

    @Value("${mercadopago.access-token}")
    private String accessToken;

    @Value("${mercadopago.webhook-secret}")
    private String webhookSecret;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    static String codigoExterno(UUID id) {
        ByteBuffer bytes = ByteBuffer.allocate(16);
        bytes.putLong(id.getMostSignificantBits());
        bytes.putLong(id.getLeastSignificantBits());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.array());
    }

    public OrderCriada criarOrder(Pedido pedido, int minutosValidade) {
        ObjectNode corpo = mapper.createObjectNode();
        corpo.put("type", "online");
        corpo.put("processing_mode", "manual");
        corpo.put("total_amount", pedido.getTotal().toPlainString());
        corpo.put("external_reference", String.valueOf(pedido.getId()));
        corpo.put("expiration_time", "PT" + minutosValidade + "M");

        ArrayNode items = corpo.putArray("items");
        for (ItemPedido ip : pedido.getItens()) {
            ObjectNode item = items.addObject();
            item.put("title", ip.getLivro().getTitulo());
            item.put("unit_price", ip.getPrecoUnitario().toPlainString());
            item.put("quantity", ip.getQuantidade());
            item.put("external_code", codigoExterno(ip.getLivro().getId()));
        }

        ObjectNode online = corpo.putObject("config").putObject("online");
        online.put("success_url", frontendUrl + "/compra/sucesso");
        online.put("pending_url", frontendUrl + "/compra/pendente");
        online.put("failure_url", frontendUrl + "/compra/falha");
        online.put("auto_return", "approved");

        HttpResponse<String> resposta = chamar(HttpRequest.newBuilder()
                .uri(URI.create(API))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .header("X-Idempotency-Key", UUID.randomUUID().toString())
                .POST(HttpRequest.BodyPublishers.ofString(corpo.toString()))
                .build());

        if (resposta.statusCode() < 200 || resposta.statusCode() >= 300) {
            log.error("Mercado Pago recusou a criação da order ({}): {}",
                    resposta.statusCode(), resposta.body());
            throw new RuntimeException("Falha ao criar o pagamento");
        }

        JsonNode json = lerJson(resposta.body());
        String checkoutUrl = json.path("checkout_url").asText();
        if (checkoutUrl.isBlank()) {
            log.error("Mercado Pago não retornou checkout_url para o pedido {}", pedido.getId());
            throw new RuntimeException("Falha ao criar o pagamento");
        }
        return new OrderCriada(json.path("id").asText(), checkoutUrl);
    }

    public OrderMP buscarOrder(String orderId) {
        if (orderId == null || !orderId.matches("[A-Za-z0-9]+")) return null;

        HttpResponse<String> resposta = chamar(HttpRequest.newBuilder()
                .uri(URI.create(API + "/" + orderId))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build());

        if (resposta.statusCode() == 404) return null;
        if (resposta.statusCode() < 200 || resposta.statusCode() >= 300) {
            log.error("Falha ao consultar a order {} ({}): {}",
                    orderId, resposta.statusCode(), resposta.body());
            throw new RuntimeException("Falha ao consultar o pagamento");
        }

        JsonNode json = lerJson(resposta.body());
        return new OrderMP(json.path("id").asText(),
                json.path("external_reference").asText(null),
                json.path("status").asText(),
                json.path("status_detail").asText());
    }

    public boolean assinaturaValida(String assinatura, String requestId, String dataId) {
        if (assinatura == null || requestId == null || dataId == null) return false;

        String ts = null;
        String v1 = null;
        for (String parte : assinatura.split(",")) {
            String[] kv = parte.trim().split("=", 2);
            if (kv.length == 2) {
                if (kv[0].equals("ts")) ts = kv[1];
                if (kv[0].equals("v1")) v1 = kv[1];
            }
        }
        if (ts == null || v1 == null) return false;

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            for (String id : List.of(dataId.toLowerCase(Locale.ROOT), dataId)) {
                String manifesto = "id:" + id + ";request-id:" + requestId + ";ts:" + ts + ";";
                String esperado = HexFormat.of().formatHex(
                        mac.doFinal(manifesto.getBytes(StandardCharsets.UTF_8)));
                if (MessageDigest.isEqual(
                        esperado.getBytes(StandardCharsets.UTF_8), v1.getBytes(StandardCharsets.UTF_8))) {
                    return true;
                }
            }
            return false;
        } catch (GeneralSecurityException e) {
            return false;
        }
    }

    private HttpResponse<String> chamar(HttpRequest request) {
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new RuntimeException("Falha na comunicação com o Mercado Pago", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Comunicação com o Mercado Pago interrompida", e);
        }
    }

    private JsonNode lerJson(String corpo) {
        try {
            return mapper.readTree(corpo);
        } catch (IOException e) {
            throw new RuntimeException("Resposta inválida do Mercado Pago", e);
        }
    }
}
