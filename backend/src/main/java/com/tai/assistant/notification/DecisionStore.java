package com.tai.assistant.notification;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Guarda las decisiones (aprobar/descartar) tomadas sobre un setup, sin importar
 * si vinieron de Telegram (TAI-12) o del frontend web (TAI-14) — un solo lugar
 * compartido. Por ahora en memoria; persistirlo junto al historial completo
 * es alcance de TAI-15.
 */
@Component
public class DecisionStore {

    private final List<SetupDecision> decisions = new CopyOnWriteArrayList<>();

    public void add(String symbol, String decision, String source) {
        decisions.add(new SetupDecision(symbol, decision, Instant.now(), source));
    }

    public List<SetupDecision> getRecent() {
        return decisions;
    }
}
