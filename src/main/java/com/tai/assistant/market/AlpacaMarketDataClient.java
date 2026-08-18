package com.tai.assistant.market;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tai.assistant.config.AlpacaProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente de datos de mercado de Alpaca — acciones y cripto.
 *
 * Nota de diseño (ver TAI-7): el endpoint multi-símbolo de Alpaca tiene reportes
 * de comportamiento inconsistente en la comunidad (a veces devuelve solo un símbolo
 * de los pedidos). Por eso {@link #getLatestStockQuotes} y {@link #getLatestCryptoQuotes}
 * hacen UNA REQUEST POR SÍMBOLO (no multi-símbolo), que es más lento pero confiable.
 * Con el universo definido (~160 símbolos) y un ciclo de escaneo de 2-3 minutos,
 * esto entra cómodo en el límite de 200 requests/minuto — ver el cálculo en
 * 07-discovery-personal.md, sección 4.3.
 *
 * Quien llame a estos métodos en bucle (TAI-10, detección de setups) es responsable
 * de programar el ciclo completo cada 2-3 minutos, no más seguido.
 */
@Component
public class AlpacaMarketDataClient {

    private final RestClient restClient;
    private final AlpacaProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AlpacaMarketDataClient(RestClient restClient, AlpacaProperties props) {
        this.restClient = restClient;
        this.props = props;
    }

    public boolean isConfigured() {
        return props.isConfigured();
    }

    // ---------- Acciones ----------

    public Quote getLatestStockQuote(String symbol) {
        String body = get(props.getDataBaseUrl() + "/v2/stocks/" + symbol + "/quotes/latest");
        try {
            JsonNode quote = objectMapper.readTree(body).path("quote");
            return new Quote(symbol, quote.path("bp").asDouble(), quote.path("ap").asDouble(),
                    quote.path("t").asText());
        } catch (Exception e) {
            throw new RuntimeException("No se pudo parsear el quote de " + symbol, e);
        }
    }

    /**
     * Trae el último precio de cada símbolo, uno por request (ver nota de la clase).
     * Devuelve un mapa símbolo -> Quote; si algún símbolo falla, se omite (no rompe el resto).
     */
    public Map<String, Quote> getLatestStockQuotes(List<String> symbols) {
        Map<String, Quote> result = new LinkedHashMap<>();
        for (String symbol : symbols) {
            try {
                result.put(symbol, getLatestStockQuote(symbol));
            } catch (Exception e) {
                // Un símbolo que falla no debe tirar abajo el escaneo completo del universo.
                System.err.println("[AlpacaMarketDataClient] Error trayendo quote de " + symbol + ": " + e.getMessage());
            }
        }
        return result;
    }

    public List<Bar> getStockBars(String symbol, int limit) {
        String body = get(props.getDataBaseUrl() + "/v2/stocks/" + symbol
                + "/bars?timeframe=1Day&limit=" + limit);
        return parseBarsArray(body.contains("\"bars\"") ? body : body, symbol, false);
    }

    // ---------- Cripto ----------
    // Alpaca usa un formato de símbolo distinto para cripto: "BTC/USD", no "BTCUSD".

    public Quote getLatestCryptoQuote(String symbol) {
        String body = get(props.getDataBaseUrl() + "/v1beta3/crypto/us/latest/quotes?symbols=" + encode(symbol));
        try {
            JsonNode quote = objectMapper.readTree(body).path("quotes").path(symbol);
            return new Quote(symbol, quote.path("bp").asDouble(), quote.path("ap").asDouble(),
                    quote.path("t").asText());
        } catch (Exception e) {
            throw new RuntimeException("No se pudo parsear el quote cripto de " + symbol, e);
        }
    }

    public Map<String, Quote> getLatestCryptoQuotes(List<String> symbols) {
        Map<String, Quote> result = new LinkedHashMap<>();
        for (String symbol : symbols) {
            try {
                result.put(symbol, getLatestCryptoQuote(symbol));
            } catch (Exception e) {
                System.err.println("[AlpacaMarketDataClient] Error trayendo quote cripto de " + symbol + ": " + e.getMessage());
            }
        }
        return result;
    }

    public List<Bar> getCryptoBars(String symbol, int limit) {
        String body = get(props.getDataBaseUrl() + "/v1beta3/crypto/us/bars?symbols=" + encode(symbol)
                + "&timeframe=1Day&limit=" + limit);
        try {
            JsonNode barsForSymbol = objectMapper.readTree(body).path("bars").path(symbol);
            return parseBarsNode(barsForSymbol);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo parsear las barras cripto de " + symbol, e);
        }
    }

    // ---------- Utilidades ----------

    private String get(String url) {
        // Se pasa como java.net.URI (no como String) a propósito: si le pasamos un String
        // ya codificado (ej. con %2F para el símbolo cripto "BTC/USD"), RestClient lo vuelve
        // a codificar una segunda vez (el "%" se convierte en "%25"), y el símbolo le llega
        // mal a Alpaca. Pasando un URI ya construido, Spring no lo re-codifica.
        return restClient.get()
                .uri(java.net.URI.create(url))
                .header("APCA-API-KEY-ID", props.getApiKey())
                .header("APCA-API-SECRET-KEY", props.getSecretKey())
                .retrieve()
                .body(String.class);
    }

    private List<Bar> parseBarsArray(String body, String symbol, boolean ignored) {
        try {
            JsonNode barsNode = objectMapper.readTree(body).path("bars");
            return parseBarsNode(barsNode);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo parsear las barras de " + symbol, e);
        }
    }

    private List<Bar> parseBarsNode(JsonNode barsNode) {
        List<Bar> bars = new ArrayList<>();
        if (barsNode.isArray()) {
            for (JsonNode b : barsNode) {
                bars.add(new Bar(
                        b.path("t").asText(),
                        b.path("o").asDouble(),
                        b.path("h").asDouble(),
                        b.path("l").asDouble(),
                        b.path("c").asDouble(),
                        b.path("v").asLong()
                ));
            }
        }
        return bars;
    }

    private String encode(String symbol) {
        return java.net.URLEncoder.encode(symbol, java.nio.charset.StandardCharsets.UTF_8);
    }
}
