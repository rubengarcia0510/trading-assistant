package com.tai.assistant.detection;

import com.tai.assistant.notification.TelegramNotifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * GET  /setups        -> últimos setups detectados (del último scan, en memoria)
 * POST /setups/scan   -> fuerza un escaneo completo del universo ahora mismo
 */
@RestController
@RequestMapping("/setups")
public class SetupController {

    private final SetupDetectionService service;
    private final SetupExplanationService explanationService;
    private final TelegramNotifier telegramNotifier;

    public SetupController(SetupDetectionService service, SetupExplanationService explanationService,
                            TelegramNotifier telegramNotifier) {
        this.service = service;
        this.explanationService = explanationService;
        this.telegramNotifier = telegramNotifier;
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

    /**
     * Endpoint de prueba: genera un Setup de ejemplo con datos inventados, le pide
     * al LLM que lo explique, Y manda el aviso por Telegram — sirve para confirmar
     * que TODO el pipeline (LLM + notificación) funciona de punta a punta sin
     * depender de que el análisis técnico encuentre una señal real.
     */
    @GetMapping("/test-explanation")
    public Map<String, Object> testExplanation() {
        Setup ejemplo = new Setup(
                "AAPL",
                Setup.AssetType.STOCK,
                java.time.Instant.now(),
                230.50,
                223.59,
                244.33,
                Setup.RiskLevel.MEDIO,
                "SMA(9) cruzó por encima de SMA(21) — señal alcista (DATOS DE PRUEBA)"
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
