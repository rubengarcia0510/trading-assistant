package com.tai.assistant.universe;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * Documento de MongoDB para el universo de activos. Siempre se guarda con
 * id="current" — no se acumula histórico de universos, cada refresh
 * SOBRESCRIBE el documento anterior (mismo comportamiento que tenía el
 * archivo JSON local, solo que ahora vive en Atlas).
 */
@Document(collection = "universe")
public record UniverseDocument(
        @Id String id,
        List<String> stockSymbols,
        List<String> cryptoSymbols,
        Instant calculatedAt
) {
    public static final String CURRENT_ID = "current";

    public static UniverseDocument of(AssetUniverse universe) {
        return new UniverseDocument(CURRENT_ID, universe.stockSymbols(), universe.cryptoSymbols(), universe.calculatedAt());
    }

    public AssetUniverse toDomain() {
        return new AssetUniverse(stockSymbols, cryptoSymbols, calculatedAt);
    }
}
