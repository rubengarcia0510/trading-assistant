package com.tai.assistant.detection;

import java.time.Instant;
import java.util.List;

/**
 * Un setup detectado por el análisis técnico. recentCloses son los últimos
 * cierres usados para el análisis — se incluyen para que el frontend pueda
 * dibujar un sparkline simple sin tener que pedir las barras de nuevo (TAI-14).
 */
public record Setup(
        String symbol,
        AssetType assetType,
        Instant detectedAt,
        double entryPrice,
        double stopLoss,
        double takeProfit,
        RiskLevel riskLevel,
        String signalDescription,
        List<Double> recentCloses
) {
    public enum AssetType { STOCK, CRYPTO }

    public enum RiskLevel { BAJO, MEDIO, ALTO }
}
