package com.tai.assistant.detection;

import com.tai.assistant.history.HistoryService;
import com.tai.assistant.market.AlpacaMarketDataClient;
import com.tai.assistant.market.Bar;
import com.tai.assistant.notification.TelegramNotifier;
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
 * Cada setup encontrado: se explica con IA, se manda por Telegram, y se persiste
 * en el historial (TAI-15) como PENDIENTE — hasta que se apruebe/descarte.
 */
@Service
public class SetupDetectionService {

    private static final int BARS_FOR_ANALYSIS = 30;

    private final UniverseService universeService;
    private final AlpacaMarketDataClient marketDataClient;
    private final TechnicalAnalysisService technicalAnalysisService;
    private final SetupExplanationService explanationService;
    private final TelegramNotifier telegramNotifier;
    private final HistoryService historyService;

    private final AtomicReference<List<ExplainedSetup>> lastSetups = new AtomicReference<>(List.of());
    private final AtomicReference<Instant> lastScanAt = new AtomicReference<>();

    public SetupDetectionService(UniverseService universeService,
                                  AlpacaMarketDataClient marketDataClient,
                                  TechnicalAnalysisService technicalAnalysisService,
                                  SetupExplanationService explanationService,
                                  TelegramNotifier telegramNotifier,
                                  HistoryService historyService) {
        this.universeService = universeService;
        this.marketDataClient = marketDataClient;
        this.technicalAnalysisService = technicalAnalysisService;
        this.explanationService = explanationService;
        this.telegramNotifier = telegramNotifier;
        this.historyService = historyService;
    }

    public List<ExplainedSetup> getLastSetups() {
        return lastSetups.get();
    }

    public Instant getLastScanAt() {
        return lastScanAt.get();
    }

    public List<ExplainedSetup> scanNow() {
        AssetUniverse universe = universeService.getCurrent();
        List<ExplainedSetup> found = new ArrayList<>();

        for (String symbol : universe.stockSymbols()) {
            scanOne(symbol, Setup.AssetType.STOCK, marketDataClient.getStockBars(symbol, BARS_FOR_ANALYSIS))
                    .ifPresent(setup -> found.add(explainNotifyAndRecord(setup)));
        }
        for (String symbol : universe.cryptoSymbols()) {
            scanOne(symbol, Setup.AssetType.CRYPTO, marketDataClient.getCryptoBars(symbol, BARS_FOR_ANALYSIS))
                    .ifPresent(setup -> found.add(explainNotifyAndRecord(setup)));
        }

        lastSetups.set(found);
        lastScanAt.set(Instant.now());
        return found;
    }

    private ExplainedSetup explainNotifyAndRecord(Setup setup) {
        ExplainedSetup explained = explanationService.explain(setup);

        try {
            historyService.recordDetection(explained);
        } catch (Exception e) {
            // No persistir no debería tirar abajo la detección ni el aviso.
            System.err.println("[SetupDetectionService] Error guardando en historial " + setup.symbol() + ": " + e.getMessage());
        }

        if (telegramNotifier.isConfigured()) {
            try {
                telegramNotifier.sendSetupAlert(explained);
            } catch (Exception e) {
                System.err.println("[SetupDetectionService] Error mandando aviso de Telegram para "
                        + setup.symbol() + ": " + e.getMessage());
            }
        }
        return explained;
    }

    private java.util.Optional<Setup> scanOne(String symbol, Setup.AssetType type, List<Bar> bars) {
        try {
            return technicalAnalysisService.analyze(symbol, type, bars);
        } catch (Exception e) {
            System.err.println("[SetupDetectionService] Error analizando " + symbol + ": " + e.getMessage());
            return java.util.Optional.empty();
        }
    }

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
