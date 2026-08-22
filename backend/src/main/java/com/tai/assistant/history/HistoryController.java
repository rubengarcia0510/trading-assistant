package com.tai.assistant.history;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * GET /history?symbol=&from=&to=&page=&size=
 * Todos los filtros son opcionales y combinables. Protegido por JWT (SecurityConfig
 * no lo excluye) — como este proyecto es de un solo usuario, "los datos corresponden
 * al usuario autenticado" queda satisfecho por el hecho de que solo existe ese usuario.
 */
@RestController
@RequestMapping("/history")
public class HistoryController {

    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping
    public PageResult<HistoryEntry> search(
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return historyService.search(symbol, from, to, page, size);
    }
}
