package com.tai.assistant.trading;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tai.assistant.config.AlpacaProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Cliente de trading de Alpaca — cuenta, posiciones, y colocación de órdenes.
 *
 * IMPORTANTE (decisión del Discovery, sección "Fuera del MVP"): este cliente NO se llama
 * automáticamente desde ningún proceso de detección de setups. {@link #placeMarketOrder}
 * existe para que VOS lo dispares manualmente (desde un endpoint, un botón en el frontend,
 * o el bot de Telegram) después de revisar un setup — nunca en background sin tu aprobación.
 *
 * Apunta a tradingBaseUrl, que en application.properties está seteado a la URL de PAPER
 * TRADING por default (https://paper-api.alpaca.markets) — cambiar a la URL real
 * (https://api.alpaca.markets) es una decisión consciente aparte, no el default.
 */
@Component
public class AlpacaTradingClient {

    private final RestClient restClient;
    private final AlpacaProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AlpacaTradingClient(RestClient restClient, AlpacaProperties props) {
        this.restClient = restClient;
        this.props = props;
    }

    public boolean isConfigured() {
        return props.isConfigured();
    }

    public Account getAccount() {
        String body = get("/v2/account");
        try {
            JsonNode n = objectMapper.readTree(body);
            return new Account(
                    n.path("id").asText(),
                    n.path("status").asText(),
                    n.path("cash").asDouble(),
                    n.path("portfolio_value").asDouble(),
                    n.path("trading_blocked").asBoolean()
            );
        } catch (Exception e) {
            throw new RuntimeException("No se pudo parsear la cuenta de Alpaca", e);
        }
    }

    public List<Position> getOpenPositions() {
        String body = get("/v2/positions");
        List<Position> positions = new ArrayList<>();
        try {
            JsonNode arr = objectMapper.readTree(body);
            for (JsonNode p : arr) {
                positions.add(new Position(
                        p.path("symbol").asText(),
                        p.path("qty").asDouble(),
                        p.path("avg_entry_price").asDouble(),
                        p.path("current_price").asDouble(),
                        p.path("unrealized_pl").asDouble()
                ));
            }
        } catch (Exception e) {
            throw new RuntimeException("No se pudieron parsear las posiciones de Alpaca", e);
        }
        return positions;
    }

    /**
     * Coloca una orden de mercado. Uso manual únicamente (ver nota de la clase) —
     * no se invoca desde ningún proceso automático de detección de setups.
     *
     * @param symbol símbolo (ej. "AAPL" o "BTC/USD")
     * @param qty    cantidad a comprar/vender
     * @param side   "buy" o "sell"
     */
    public String placeMarketOrder(String symbol, double qty, String side) {
        Map<String, Object> orderBody = Map.of(
                "symbol", symbol,
                "qty", qty,
                "side", side,
                "type", "market",
                "time_in_force", "day"
        );
        return restClient.post()
                .uri(props.getTradingBaseUrl() + "/v2/orders")
                .header("APCA-API-KEY-ID", props.getApiKey())
                .header("APCA-API-SECRET-KEY", props.getSecretKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(orderBody)
                .retrieve()
                .body(String.class);
    }

    private String get(String path) {
        return restClient.get()
                .uri(props.getTradingBaseUrl() + path)
                .header("APCA-API-KEY-ID", props.getApiKey())
                .header("APCA-API-SECRET-KEY", props.getSecretKey())
                .retrieve()
                .body(String.class);
    }
}
