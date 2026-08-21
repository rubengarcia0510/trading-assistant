package com.tai.assistant.detection;

import java.time.Instant;

/**
 * Un setup detectado por el análisis técnico. Estos campos son justamente los que
 * pide el SRS original (RF-03.3): precio de entrada, stop-loss, take-profit, nivel de riesgo.
 * TAI-11 (Spring AI) va a tomar este objeto y convertirlo en la explicación en lenguaje simple.
 */
public record Setup(
        String symbol,
        AssetType assetType,
        Instant detectedAt,
        double entryPrice,
        double stopLoss,
        double takeProfit,
        RiskLevel riskLevel,
        String signalDescription
) {
    public enum AssetType { STOCK, CRYPTO }

    public enum RiskLevel { BAJO, MEDIO, ALTO }
}
