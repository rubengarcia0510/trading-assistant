package com.tai.assistant.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tai.assistant.config.TelegramProperties;
import com.tai.assistant.history.HistoryService;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Escucha cuándo se presiona "Aprobar" o "Descartar" en el bot de Telegram.
 * Usa long polling (GET .../getUpdates) en vez de webhook — no hace falta
 * una URL pública HTTPS para una app corriendo en el celular.
 *
 * Las decisiones se persisten en HistoryService (Mongo) — TAI-15.
 */
@Component
public class TelegramCallbackPoller {

    private final RestClient restClient;
    private final TelegramProperties props;
    private final HistoryService historyService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final AtomicLong lastUpdateId = new AtomicLong(0);

    public TelegramCallbackPoller(RestClient restClient, TelegramProperties props, HistoryService historyService) {
        this.restClient = restClient;
        this.props = props;
        this.historyService = historyService;
    }

    @Scheduled(fixedDelayString = "${tai.telegram.poll-interval-ms:5000}")
    public void pollForCallbacks() {
        if (!props.isConfigured()) {
            return;
        }
        try {
            String url = "https://api.telegram.org/bot" + props.getBotToken()
                    + "/getUpdates?offset=" + (lastUpdateId.get() + 1) + "&timeout=0";
            String body = restClient.get().uri(url).retrieve().body(String.class);

            JsonNode results = objectMapper.readTree(body).path("result");
            for (JsonNode update : results) {
                long updateId = update.path("update_id").asLong();
                lastUpdateId.set(Math.max(lastUpdateId.get(), updateId));

                JsonNode callback = update.path("callback_query");
                if (!callback.isMissingNode()) {
                    handleCallback(callback);
                }
            }
        } catch (Exception e) {
            System.err.println("[TelegramCallbackPoller] Error consultando updates: " + e.getMessage());
        }
    }

    private void handleCallback(JsonNode callback) {
        String callbackQueryId = callback.path("id").asText();
        String data = callback.path("data").asText(); // ej: "approve:AAPL:1755600000000"
        String[] parts = data.split(":");
        if (parts.length < 2) return;

        String action = parts[0]; // "approve" o "discard"
        String symbol = parts[1];
        String decisionLabel = action.equals("approve") ? "✅ Aprobado" : "❌ Descartado";

        historyService.recordDecision(symbol, action, "telegram");
        System.out.println("[TelegramCallbackPoller] Decisión registrada: " + symbol + " -> " + action);

        answerCallbackQuery(callbackQueryId, decisionLabel);
        editMessageToShowDecision(callback, decisionLabel);
    }

    private void answerCallbackQuery(String callbackQueryId, String text) {
        Map<String, Object> body = Map.of("callback_query_id", callbackQueryId, "text", text);
        restClient.post()
                .uri("https://api.telegram.org/bot" + props.getBotToken() + "/answerCallbackQuery")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    private void editMessageToShowDecision(JsonNode callback, String decisionLabel) {
        JsonNode message = callback.path("message");
        long chatId = message.path("chat").path("id").asLong();
        long messageId = message.path("message_id").asLong();
        String originalText = message.path("text").asText();

        Map<String, Object> body = new java.util.HashMap<>();
        body.put("chat_id", chatId);
        body.put("message_id", messageId);
        body.put("text", originalText + "\n\n" + decisionLabel);

        restClient.post()
                .uri("https://api.telegram.org/bot" + props.getBotToken() + "/editMessageText")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }
}
