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

    public SetupController(SetupDetectionService service) {
        this.service = service;
    }

    @GetMapping
    public Map<String, Object> lastSetups() {
        Instant lastScan = service.getLastScanAt();
        List<Setup> setups = service.getLastSetups();
        return Map.of(
                "lastScanAt", lastScan == null ? "todavía no se escaneó" : lastScan.toString(),
                "count", setups.size(),
                "setups", setups
        );
    }

    @PostMapping("/scan")
    public List<Setup> scanNow() {
        return service.scanNow();
    }
}
