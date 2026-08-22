package com.tai.assistant.notification;

import java.time.Instant;

/** Decisión tomada sobre un setup — desde Telegram o desde el frontend web (TAI-14). */
public record SetupDecision(String symbol, String decision, Instant decidedAt, String source) {
}
