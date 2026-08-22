package com.tai.assistant.detection;

import com.tai.assistant.notification.DecisionStore;
import com.tai.assistant.notification.TelegramNotifier;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * GET  /setups            -> últimos setups detectados (del último scan, en memoria)
 * POST /setups/scan       -> fuerza un escaneo completo del universo ahora mismo
 * POST /setups/decision   -> registra una decisión (aprobar/descartar) desde el frontend web (TAI-14)
 */
@RestController
@RequestMapping("/setups")
public class SetupController {

    private final SetupDetectionService service;
    private final SetupExplanationService explanationService;
    private final TelegramNotifier telegramNotifier;
    private final DecisionStore decisionStore;

    public SetupController(SetupDetectionService service, SetupExplanationService explanationService,
                            TelegramNotifier telegramNotifier, DecisionStore decisionStore) {
        this.service = service;
        this.explanationService = explanationService;
        this.telegramNotifier = telegramNotifier;
        this.decisionStore = decisionStore;
    }

    @GetMapping
    public Map<String, Object> lastSetups() {
        Instant lastScan = service.getLastScanAt();
        List<ExplainedSetup> setups = service.getLastSetups();
        return Map.of(
                "lastScanAt", lastScan == null ? "todavía no se escaneó" : lastScan.toString(),
                "count", setups.size(),
                "setups", setups
        );
    }

    @PostMapping("/scan")
    public List<ExplainedSetup> scanNow() {
        return service.scanNow();
    }

    /** Body: {"symbol": "AAPL", "decision": "approve"} (o "discard"). */
    @PostMapping("/decision")
    public Map<String, String> registerDecision(@RequestBody Map<String, String> body) {
        String symbol = body.get("symbol");
        String decision = body.get("decision");
        decisionStore.add(symbol, decision, "web");
        return Map.of("status", "ok");
    }

    /**
     * Endpoint de prueba: genera un Setup de ejemplo con datos inventados, le pide
     * al LLM que lo explique, Y manda el aviso por Telegram — sirve para confirmar
     * que TODO el pipeline funciona sin depender de una señal real.
     */
    @GetMapping("/test-explanation")
    public Map<String, Object> testExplanation() {
        Setup ejemplo = new Setup(
                "AAPL",
                Setup.AssetType.STOCK,
                Instant.now(),
                230.50,
                223.59,
                244.33,
                Setup.RiskLevel.MEDIO,
                "SMA(9) cruzó por encima de SMA(21) — señal alcista (DATOS DE PRUEBA)",
                List.of(225.1, 226.4, 224.8, 227.2, 228.0, 229.5, 227.8, 230.1, 229.0, 230.5)
        );
        ExplainedSetup explained = explanationService.explain(ejemplo);

        boolean telegramSent = false;
        String telegramError = null;
        if (telegramNotifier.isConfigured()) {
            try {
                telegramNotifier.sendSetupAlert(explained);
                telegramSent = true;
            } catch (Exception e) {
                telegramError = e.getMessage();
            }
        }

        return Map.of(
                "explainedSetup", explained,
                "telegramConfigured", telegramNotifier.isConfigured(),
                "telegramSent", telegramSent,
                "telegramError", telegramError == null ? "" : telegramError
        );
    }
}
