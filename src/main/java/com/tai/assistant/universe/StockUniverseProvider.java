package com.tai.assistant.universe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tai.assistant.config.FinnhubProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Top N acciones por capitalización de mercado.
 *
 * Dos fuentes combinadas (FMP jubiló su endpoint de screener en el plan gratuito,
 * así que se arma "a mano" con dos piezas que sí son gratis):
 *  1. Lista de símbolos candidatos: constituyentes del S&P 500, desde un CSV público
 *     en GitHub (sin API key, se actualiza cuando el mantenedor del dataset lo actualiza).
 *  2. Capitalización real de cada símbolo: Finnhub (60 requests/minuto gratis, sin tarjeta).
 *
 * Con ~500 símbolos y el límite de 60 req/min, se pacea explícitamente (ver PACE_MS)
 * para no pasarse — el recálculo completo tarda unos 9-10 minutos. Como se corre
 * una vez al día (ver UniverseService), no es un problema.
 */
@Component
public class StockUniverseProvider {

    private static final String SP500_CSV_URL =
            "https://raw.githubusercontent.com/datasets/s-and-p-500-companies/master/data/constituents.csv";

    private static final long PACE_MS = 1100; // ~54 req/min, con margen bajo el límite de 60/min de Finnhub

    private final RestClient restClient;
    private final FinnhubProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StockUniverseProvider(RestClient restClient, FinnhubProperties props) {
        this.restClient = restClient;
        this.props = props;
    }

    public boolean isConfigured() {
        return props.isConfigured();
    }

    /** Devuelve hasta {@code topN} símbolos, ordenados de mayor a menor capitalización. */
    public List<String> fetchTopByMarketCap(int topN) {
        List<String> candidates = fetchSp500Symbols();
        List<SymbolCap> ranked = new ArrayList<>();

        for (String symbol : candidates) {
            try {
                double marketCap = fetchMarketCap(symbol);
                if (marketCap > 0) {
                    ranked.add(new SymbolCap(symbol, marketCap));
                }
            } catch (Exception e) {
                System.err.println("[StockUniverseProvider] Error trayendo market cap de " + symbol + ": " + e.getMessage());
            }
            sleep(PACE_MS);
        }

        return ranked.stream()
                .sorted(Comparator.comparingDouble(SymbolCap::marketCap).reversed())
                .limit(topN)
                .map(SymbolCap::symbol)
                .toList();
    }

    /** Lista de símbolos del S&P 500 — universo candidato, sin API key. */
    private List<String> fetchSp500Symbols() {
        String csv = restClient.get()
                .uri(java.net.URI.create(SP500_CSV_URL))
                .retrieve()
                .body(String.class);

        List<String> symbols = new ArrayList<>();
        String[] lines = csv.split("\n");
        for (int i = 1; i < lines.length; i++) { // línea 0 es el header (Symbol,Security,...)
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            String symbol = line.split(",")[0].trim();
            if (!symbol.isEmpty()) {
                symbols.add(symbol);
            }
        }
        return symbols;
    }

    /** Capitalización de mercado en USD (Finnhub la devuelve en millones, se convierte acá). */
    private double fetchMarketCap(String symbol) {
        String url = props.getBaseUrl() + "/stock/profile2?symbol=" + symbol + "&token=" + props.getApiKey();
        String body = restClient.get()
                .uri(java.net.URI.create(url))
                .retrieve()
                .body(String.class);
        try {
            JsonNode n = objectMapper.readTree(body);
            double marketCapMillions = n.path("marketCapitalization").asDouble(0);
            return marketCapMillions * 1_000_000;
        } catch (Exception e) {
            throw new RuntimeException("No se pudo parsear el perfil de " + symbol + " en Finnhub", e);
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }

    private record SymbolCap(String symbol, double marketCap) {
    }
}
