package com.tai.assistant.market;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Endpoints de prueba manual para TAI-8, mientras no hay frontend (TAI-14/15).
 * Ejemplos:
 *   GET /market/stocks/quote/AAPL
 *   GET /market/stocks/quotes?symbols=AAPL,MSFT,GOOGL
 *   GET /market/stocks/bars/AAPL?limit=20
 *   GET /market/crypto/quote?symbol=BTC/USD
 *
 * Nota: los símbolos cripto de Alpaca llevan "/" (ej. "BTC/USD"). Tomcat rechaza por
 * default una barra codificada (%2F) dentro del path, así que el símbolo cripto va
 * como query param (?symbol=...), no como parte del path — evita el problema de raíz.
 */
@RestController
public class MarketDataController {

    private final AlpacaMarketDataClient client;

    public MarketDataController(AlpacaMarketDataClient client) {
        this.client = client;
    }

    @GetMapping("/market/stocks/quote/{symbol}")
    public Quote stockQuote(@PathVariable String symbol) {
        return client.getLatestStockQuote(symbol);
    }

    @GetMapping("/market/stocks/quotes")
    public Map<String, Quote> stockQuotes(@RequestParam List<String> symbols) {
        return client.getLatestStockQuotes(symbols);
    }

    @GetMapping("/market/stocks/bars/{symbol}")
    public List<Bar> stockBars(@PathVariable String symbol, @RequestParam(defaultValue = "20") int limit) {
        return client.getStockBars(symbol, limit);
    }

    @GetMapping("/market/crypto/quote")
    public Quote cryptoQuote(@RequestParam String symbol) {
        return client.getLatestCryptoQuote(symbol);
    }

    @GetMapping("/market/crypto/bars")
    public List<Bar> cryptoBars(@RequestParam String symbol, @RequestParam(defaultValue = "20") int limit) {
        return client.getCryptoBars(symbol, limit);
    }
}
