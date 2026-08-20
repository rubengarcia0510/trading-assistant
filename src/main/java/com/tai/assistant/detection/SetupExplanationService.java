package com.tai.assistant.detection;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Convierte un Setup técnico (SMA, stop-loss, etc.) en una explicación en
 * lenguaje cotidiano — el equivalente en código a RF-03.2 del SRS original:
 * "NVIDIA muestra un patrón alcista con +2.5% estimado. ¿Querés invertir $20 USD?"
 *
 * Usa Spring AI con el ChatClient autoconfigurado (apunta a Groq, ver application.properties).
 * Solo se llama para setups que YA se detectaron (no en cada símbolo escaneado) —
 * mantiene bajo el volumen de llamadas al LLM, según lo calculado en TAI-6/TAI-7.
 */
@Service
public class SetupExplanationService {

    private final ChatClient chatClient;

    public SetupExplanationService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public ExplainedSetup explain(Setup setup) {
        String prompt = buildPrompt(setup);
        String explanation;
        try {
            explanation = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            // Si el LLM falla (rate limit, red, etc.), no se pierde el setup detectado —
            // se devuelve sin explicación en lenguaje natural, pero con todos los datos técnicos.
            System.err.println("[SetupExplanationService] Error generando explicación para "
                    + setup.symbol() + ": " + e.getMessage());
            explanation = "(No se pudo generar la explicación en este momento — ver datos técnicos.)";
        }
        return new ExplainedSetup(setup, explanation);
    }

    private String buildPrompt(Setup setup) {
        return """
                Sos un asistente que explica oportunidades de inversión en lenguaje simple y cotidiano,
                para alguien sin conocimientos financieros avanzados. NO des consejo financiero ni le
                digas a la persona qué hacer — solo explicá qué se detectó, en un tono neutro y claro.

                Datos del setup detectado:
                - Activo: %s (%s)
                - Señal: %s
                - Precio de entrada de referencia: $%.2f
                - Stop-loss (límite de pérdida): $%.2f
                - Take-profit (meta de ganancia): $%.2f
                - Nivel de riesgo estimado: %s

                Escribí una explicación de 2-3 frases en español, mencionando el activo, la señal
                detectada, y los tres precios de referencia. No uses jerga innecesaria.
                """.formatted(
                setup.symbol(),
                setup.assetType(),
                setup.signalDescription(),
                setup.entryPrice(),
                setup.stopLoss(),
                setup.takeProfit(),
                setup.riskLevel()
        );
    }
}
