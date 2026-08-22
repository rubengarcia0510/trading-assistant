package com.tai.assistant.history;

import com.tai.assistant.detection.Setup;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * Registro persistente de un setup detectado + su decisión (si ya se tomó).
 * Reemplaza al DecisionStore en memoria de TAI-14 — esto es lo que faltaba
 * para cumplir el criterio "la decisión queda persistida en backend/base de datos".
 *
 * decision/decidedAt/source quedan null mientras el setup está pendiente;
 * se completan cuando el usuario aprueba/descarta (desde web o Telegram).
 */
@Document(collection = "history")
public class HistoryEntry {

    @Id
    private String id;
    private String symbol;
    private Setup.AssetType assetType;
    private double entryPrice;
    private double stopLoss;
    private double takeProfit;
    private Setup.RiskLevel riskLevel;
    private String explanation;
    private List<Double> recentCloses;
    private Instant detectedAt;

    private String decision;   // "approve" | "discard" | null (pendiente)
    private Instant decidedAt; // null mientras está pendiente
    private String source;     // "web" | "telegram" | null

    public HistoryEntry() {
    }

    public HistoryEntry(String symbol, Setup.AssetType assetType, double entryPrice, double stopLoss,
                         double takeProfit, Setup.RiskLevel riskLevel, String explanation,
                         List<Double> recentCloses, Instant detectedAt) {
        this.symbol = symbol;
        this.assetType = assetType;
        this.entryPrice = entryPrice;
        this.stopLoss = stopLoss;
        this.takeProfit = takeProfit;
        this.riskLevel = riskLevel;
        this.explanation = explanation;
        this.recentCloses = recentCloses;
        this.detectedAt = detectedAt;
    }

    // Getters y setters

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public Setup.AssetType getAssetType() { return assetType; }
    public void setAssetType(Setup.AssetType assetType) { this.assetType = assetType; }

    public double getEntryPrice() { return entryPrice; }
    public void setEntryPrice(double entryPrice) { this.entryPrice = entryPrice; }

    public double getStopLoss() { return stopLoss; }
    public void setStopLoss(double stopLoss) { this.stopLoss = stopLoss; }

    public double getTakeProfit() { return takeProfit; }
    public void setTakeProfit(double takeProfit) { this.takeProfit = takeProfit; }

    public Setup.RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(Setup.RiskLevel riskLevel) { this.riskLevel = riskLevel; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public List<Double> getRecentCloses() { return recentCloses; }
    public void setRecentCloses(List<Double> recentCloses) { this.recentCloses = recentCloses; }

    public Instant getDetectedAt() { return detectedAt; }
    public void setDetectedAt(Instant detectedAt) { this.detectedAt = detectedAt; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public Instant getDecidedAt() { return decidedAt; }
    public void setDecidedAt(Instant decidedAt) { this.decidedAt = decidedAt; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
