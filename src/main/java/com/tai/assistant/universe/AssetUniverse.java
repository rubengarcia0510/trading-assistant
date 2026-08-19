package com.tai.assistant.universe;

import java.time.Instant;
import java.util.List;

/**
 * Universo dinámico de activos a escanear (TAI-9).
 * Se recalcula solo (ver UniverseService) — no es una lista fija que haya que mantener a mano.
 */
public record AssetUniverse(List<String> stockSymbols, List<String> cryptoSymbols, Instant calculatedAt) {

    public int totalSize() {
        return stockSymbols.size() + cryptoSymbols.size();
    }
}
