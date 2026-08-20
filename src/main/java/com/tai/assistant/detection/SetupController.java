package com.tai.assistant.detection;

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

    public SetupController(SetupDetectionService service, SetupExplanationService explanationService) {
        this.service = service;
        this.explanationService = explanationService;
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
     * Endpoint de prueba: genera un Setup de ejemplo con datos inventados y le pide
     * al LLM que lo explique — sirve para confirmar que Spring AI/Groq funcionan
     * de punta a punta sin depender de que el análisis técnico encuentre una señal real.
     */
    @GetMapping("/test-explanation")
    public ExplainedSetup testExplanation() {
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
        return explanationService.explain(ejemplo);
    }
}
