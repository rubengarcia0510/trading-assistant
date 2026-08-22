package com.tai.assistant.detection;

import com.tai.assistant.market.Bar;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Análisis técnico simple: cruce de medias móviles (SMA corta por encima de SMA larga
 * = señal alcista), con stop-loss/take-profit a distancia fija, y nivel de riesgo
 * derivado de la volatilidad reciente (desvío estándar de los cierres).
 */
@Component
public class TechnicalAnalysisService {

    private static final int SHORT_SMA_PERIOD = 9;
    private static final int LONG_SMA_PERIOD = 21;
    private static final double STOP_LOSS_PCT = 0.03;   // 3% por debajo del precio de entrada
    private static final double TAKE_PROFIT_PCT = 0.06; // 6% por encima — ratio riesgo/beneficio 1:2
    private static final int SPARKLINE_POINTS = 20;      // últimos N cierres para el sparkline del frontend

    public Optional<Setup> analyze(String symbol, Setup.AssetType assetType, List<Bar> bars) {
        if (bars.size() < LONG_SMA_PERIOD + 1) {
            return Optional.empty();
        }

        double shortSmaNow = sma(bars, bars.size() - 1, SHORT_SMA_PERIOD);
        double longSmaNow = sma(bars, bars.size() - 1, LONG_SMA_PERIOD);
        double shortSmaPrev = sma(bars, bars.size() - 2, SHORT_SMA_PERIOD);
        double longSmaPrev = sma(bars, bars.size() - 2, LONG_SMA_PERIOD);

        boolean crossedUpNow = shortSmaPrev <= longSmaPrev && shortSmaNow > longSmaNow;
        if (!crossedUpNow) {
            return Optional.empty();
        }

        double entryPrice = bars.get(bars.size() - 1).close();
        double stopLoss = entryPrice * (1 - STOP_LOSS_PCT);
        double takeProfit = entryPrice * (1 + TAKE_PROFIT_PCT);
        Setup.RiskLevel risk = estimateRisk(bars);
        List<Double> recentCloses = extractRecentCloses(bars);

        String description = String.format(
                "SMA(%d) cruzó por encima de SMA(%d) — señal alcista", SHORT_SMA_PERIOD, LONG_SMA_PERIOD);

        return Optional.of(new Setup(symbol, assetType, Instant.now(), entryPrice, stopLoss, takeProfit,
                risk, description, recentCloses));
    }

    private double sma(List<Bar> bars, int endIndex, int period) {
        int start = endIndex - period + 1;
        double sum = 0;
        for (int i = start; i <= endIndex; i++) {
            sum += bars.get(i).close();
        }
        return sum / period;
    }

    private Setup.RiskLevel estimateRisk(List<Bar> bars) {
        int window = Math.min(10, bars.size());
        List<Bar> recent = bars.subList(bars.size() - window, bars.size());

        double mean = recent.stream().mapToDouble(Bar::close).average().orElse(0);
        double variance = recent.stream().mapToDouble(b -> Math.pow(b.close() - mean, 2)).average().orElse(0);
        double stdDev = Math.sqrt(variance);
        double volatilityPct = mean == 0 ? 0 : (stdDev / mean) * 100;

        if (volatilityPct < 2) return Setup.RiskLevel.BAJO;
        if (volatilityPct < 5) return Setup.RiskLevel.MEDIO;
        return Setup.RiskLevel.ALTO;
    }

    private List<Double> extractRecentCloses(List<Bar> bars) {
        int window = Math.min(SPARKLINE_POINTS, bars.size());
        return bars.subList(bars.size() - window, bars.size()).stream()
                .map(Bar::close)
                .collect(Collectors.toList());
    }
}
