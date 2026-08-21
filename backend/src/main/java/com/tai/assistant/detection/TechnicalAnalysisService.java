package com.tai.assistant.detection;

import com.tai.assistant.market.Bar;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Análisis técnico simple: cruce de medias móviles (SMA corta por encima de SMA larga
 * = señal alcista), con stop-loss/take-profit a distancia fija, y nivel de riesgo
 * derivado de la volatilidad reciente (desvío estándar de los cierres).
 *
 * Es intencionalmente simple para el MVP — nada de indicadores complejos ni ML.
 * Si más adelante hace falta algo más sofisticado, este es el lugar para reemplazarlo,
 * sin tocar el resto del pipeline (SetupDetectionService no sabe ni le importa
 * CÓMO se decide un setup, solo consume el resultado).
 */
@Component
public class TechnicalAnalysisService {

    private static final int SHORT_SMA_PERIOD = 9;
    private static final int LONG_SMA_PERIOD = 21;
    private static final double STOP_LOSS_PCT = 0.03;   // 3% por debajo del precio de entrada
    private static final double TAKE_PROFIT_PCT = 0.06; // 6% por encima — ratio riesgo/beneficio 1:2

    /**
     * Analiza las barras de un símbolo y devuelve un Setup si detecta señal alcista,
     * o Optional.empty() si no hay señal (que es el caso normal, la mayoría de las veces).
     */
    public Optional<Setup> analyze(String symbol, Setup.AssetType assetType, List<Bar> bars) {
        if (bars.size() < LONG_SMA_PERIOD + 1) {
            return Optional.empty(); // no hay suficiente historial para calcular la SMA larga
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

        String description = String.format(
                "SMA(%d) cruzó por encima de SMA(%d) — señal alcista", SHORT_SMA_PERIOD, LONG_SMA_PERIOD);

        return Optional.of(new Setup(symbol, assetType, Instant.now(), entryPrice, stopLoss, takeProfit, risk, description));
    }

    /** Media móvil simple de los últimos {@code period} cierres, terminando en el índice {@code endIndex} (inclusive). */
    private double sma(List<Bar> bars, int endIndex, int period) {
        int start = endIndex - period + 1;
        double sum = 0;
        for (int i = start; i <= endIndex; i++) {
            sum += bars.get(i).close();
        }
        return sum / period;
    }

    /**
     * Riesgo estimado a partir de la volatilidad reciente (desvío estándar de los
     * cierres de los últimos 10 días, como % del precio promedio). Es un heurístico
     * simple, no un modelo de riesgo formal — suficiente para el MVP.
     */
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
}
