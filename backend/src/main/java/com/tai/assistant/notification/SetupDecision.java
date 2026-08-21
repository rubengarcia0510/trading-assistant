package com.tai.assistant.notification;

import java.time.Instant;

/**
 * Decisión tomada desde los botones de Telegram. Por ahora solo se guarda en
 * memoria (log) — persistirlo en MongoDB junto al historial completo de setups
 * es alcance de TAI-15, no de este ticket.
 */
public record SetupDecision(String symbol, String decision, Instant decidedAt) {
}
