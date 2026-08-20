package com.tai.assistant.notification;

import com.tai.assistant.config.TelegramProperties;
import com.tai.assistant.detection.ExplainedSetup;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Manda el aviso de un setup detectado por Telegram, con dos botones inline
 * ("✅ Aprobar" / "❌ Descartar") — el "Human-in-the-loop" del SRS original (RF-05).
 * Presionar un botón NO ejecuta ninguna orden automáticamente — solo registra
 * la decisión (ver TelegramCallbackPoller); la ejecución real sigue siendo manual
 * vía POST /trading/order, tal como se definió en el Discovery.
 */
@Component
public class TelegramNotifier {

    private final RestClient restClient;
    private final TelegramProperties props;

    public TelegramNotifier(RestClient restClient, TelegramProperties props) {
        this.restClient = restClient;
        this.props = props;
    }

    public boolean isConfigured() {
        return props.isConfigured();
    }

    public void sendSetupAlert(ExplainedSetup explained) {
        String text = buildMessageText(explained);
        String callbackSuffix = explained.setup().symbol() + ":" + explained.setup().detectedAt().toEpochMilli();

        Map<String, Object> inlineKeyboard = Map.of(
                "inline_keyboard", List.of(List.of(
                        Map.of("text", "✅ Aprobar", "callback_data", "approve:" + callbackSuffix),
                        Map.of("text", "❌ Descartar", "callback_data", "discard:" + callbackSuffix)
                ))
        );

        Map<String, Object> body = Map.of(
                "chat_id", props.getChatId(),
                "text", text,
                "reply_markup", inlineKeyboard
        );

        restClient.post()
                .uri("https://api.telegram.org/bot" + props.getBotToken() + "/sendMessage")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    private String buildMessageText(ExplainedSetup explained) {
        var setup = explained.setup();
        return String.format("""
                📈 *Nuevo setup detectado: %s*

                %s

                Entrada: $%.2f
                Stop-loss: $%.2f
                Take-profit: $%.2f
                Riesgo: %s
                """,
                setup.symbol(), explained.explanation(),
                setup.entryPrice(), setup.stopLoss(), setup.takeProfit(), setup.riskLevel());
    }
}
