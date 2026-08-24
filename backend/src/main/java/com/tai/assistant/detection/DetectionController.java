package com.tai.assistant.detection;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Endpoints administrativos / de prueba para disparar acciones relacionadas con
 * la detección de setups desde fuera del scheduler o del frontend principal.
 */
@RestController
@RequestMapping("/detection")
public class DetectionController {

    private final SetupDetectionService detectionService;

    public DetectionController(SetupDetectionService detectionService) {
        this.detectionService = detectionService;
    }

    /**
     * Forza un escaneo completo del universo ahora mismo y devuelve un resumen.
     * Protegido por JWT (SecurityConfig exige autenticación por defecto).
     */
    @GetMapping("/scan-now")
    public Map<String, Object> scanNow() {
        List<ExplainedSetup> setups = detectionService.scanNow();
        Instant lastScan = detectionService.getLastScanAt();

        Map<String, Object> resp = new HashMap<>();
        resp.put("lastScanAt", lastScan == null ? "todavía no se escaneó" : lastScan.toString());
        resp.put("count", setups.size());
        resp.put("setups", setups);
        return resp;
    }
}
