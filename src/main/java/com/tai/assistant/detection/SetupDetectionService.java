package com.tai.assistant.detection;

import com.tai.assistant.market.AlpacaMarketDataClient;
import com.tai.assistant.market.Bar;
import com.tai.assistant.universe.AssetUniverse;
import com.tai.assistant.universe.UniverseService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Escanea el universo completo (acciones + cripto de UniverseService) buscando setups.
 *
 * Frecuencia: cada 3 minutos por default (ver TAI-7 en 07-discovery-personal.md) —
 * con ~160 símbolos y una request por símbolo, esto entra cómodo en el límite
 * de 200 requests/minuto de Alpaca, incluso sin necesitar el endpoint multi-símbolo
 * (que resultó poco confiable).
 *
 * Los setups detectados se guardan en memoria (no hay base de datos todavía —
 * eso queda para cuando se sume persistencia, fuera del alcance de este ticket).
 * Cada scan REEMPLAZA la lista anterior — no se acumula histórico acá.
 */
@Service
public class SetupDetectionService {

    private static final int BARS_FOR_ANALYSIS = 30; // suficiente para SMA(21) + margen

    private final UniverseService universeService;
    private final AlpacaMarketDataClient marketDataClient;
    private final TechnicalAnalysisService technicalAnalysisService;
    private final SetupExplanationService explanationService;

    private final AtomicReference<List<ExplainedSetup>> lastSetups = new AtomicReference<>(List.of());
    private final AtomicReference<Instant> lastScanAt = new AtomicReference<>();

    public SetupDetectionService(UniverseService universeService,
                                  AlpacaMarketDataClient marketDataClient,
                                  TechnicalAnalysisService technicalAnalysisService,
                                  SetupExplanationService explanationService) {
        this.universeService = universeService;
        this.marketDataClient = marketDataClient;
        this.technicalAnalysisService = technicalAnalysisService;
        this.explanationService = explanationService;
    }

    public List<ExplainedSetup> getLastSetups() {
        return lastSetups.get();
    }

    public Instant getLastScanAt() {
        return lastScanAt.get();
    }

    /** Escanea el universo completo ahora mismo. Se puede disparar manualmente (POST /setups/scan). */
    public List<ExplainedSetup> scanNow() {
        AssetUniverse universe = universeService.getCurrent();
        List<ExplainedSetup> found = new ArrayList<>();

        for (String symbol : universe.stockSymbols()) {
            scanOne(symbol, Setup.AssetType.STOCK, marketDataClient.getStockBars(symbol, BARS_FOR_ANALYSIS))
                    .ifPresent(setup -> found.add(explanationService.explain(setup)));
        }
        for (String symbol : universe.cryptoSymbols()) {
            scanOne(symbol, Setup.AssetType.CRYPTO, marketDataClient.getCryptoBars(symbol, BARS_FOR_ANALYSIS))
                    .ifPresent(setup -> found.add(explanationService.explain(setup)));
        }

        lastSetups.set(found);
        lastScanAt.set(Instant.now());
        return found;
    }

    private java.util.Optional<Setup> scanOne(String symbol, Setup.AssetType type, List<Bar> bars) {
        try {
            return technicalAnalysisService.analyze(symbol, type, bars);
        } catch (Exception e) {
            System.err.println("[SetupDetectionService] Error analizando " + symbol + ": " + e.getMessage());
            return java.util.Optional.empty();
        }
    }

    /**
     * Cada 3 minutos (180.000 ms), con 1 minuto de delay inicial para no arrancar
     * a escanear antes de que el universo tenga datos la primera vez que levanta la app.
     * Configurable con tai.detection.scan-interval-ms.
     */
    @Scheduled(fixedRateString = "${tai.detection.scan-interval-ms:180000}",
               initialDelayString = "${tai.detection.initial-delay-ms:60000}")
    public void scheduledScan() {
        if (universeService.getCurrent().totalSize() == 0) {
            System.out.println("[SetupDetectionService] Universo vacío todavía, se salta este ciclo de escaneo.");
            return;
        }
        try {
            List<ExplainedSetup> setups = scanNow();
            System.out.println("[SetupDetectionService] Scan completo: " + setups.size() + " setup(s) detectado(s).");
        } catch (Exception e) {
            System.err.println("[SetupDetectionService] Error en el scan programado: " + e.getMessage());
        }
    }
}
