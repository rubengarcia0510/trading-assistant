package com.tai.assistant.universe;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET  /universe          -> universo actual (calculado la última vez, en memoria)
 * POST /universe/refresh  -> fuerza un recálculo ahora mismo (útil para probar sin esperar al cron)
 */
@RestController
@RequestMapping("/universe")
public class UniverseController {

    private final UniverseService service;

    public UniverseController(UniverseService service) {
        this.service = service;
    }

    @GetMapping
    public AssetUniverse current() {
        return service.getCurrent();
    }

    @PostMapping("/refresh")
    public AssetUniverse refresh() {
        return service.refresh();
    }
}
