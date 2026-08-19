package com.tai.assistant.universe;

import com.tai.assistant.market.AlpacaMarketDataClient;
import com.tai.assistant.market.Bar;
import com.tai.assistant.trading.AlpacaTradingClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Top N criptomonedas por volumen de 24hs, usando los propios datos de Alpaca
 * (a diferencia de acciones, acá no hace falta una fuente externa: Alpaca ya
 * soporta un conjunto acotado de pares cripto, y trae volumen en sus barras).
 *
 * Se filtra a solo pares cotizados en USD (ej. "BTC/USD", no "LINK/BTC") — evita
 * mezclar unidades distintas (dólares vs. fracciones de otra cripto) en el mismo
 * ranking, y evita que un mismo activo compita por volumen en dos lugares distintos
 * del ranking (ej. LINK como "LINK/USD" y como base de "LINK/BTC").
 *
 * Se excluyen las stablecoins (USDC, USDT, etc.): mueven mucho volumen pero por
 * diseño no tienen variación de precio real, así que no tiene sentido "detectar
 * un setup" ahí — solo ocuparían lugares del ranking sin aportar nada útil.
 *
 * Nota: esto hace una request por símbolo del universo cripto que soporta Alpaca
 * (normalmente unas pocas decenas de pares) para traer su última barra diaria
 * y leer el volumen — mismo criterio "una request por símbolo" que en TAI-7/TAI-8.
 */
@Component
public class CryptoUniverseProvider {

    private static final Set<String> STABLECOINS = Set.of(
            "USDC", "USDT", "DAI", "BUSD", "TUSD", "USDP", "GUSD", "PYUSD", "USDG", "USDS"
    );

    private final AlpacaTradingClient tradingClient;
    private final AlpacaMarketDataClient marketDataClient;

    public CryptoUniverseProvider(AlpacaTradingClient tradingClient, AlpacaMarketDataClient marketDataClient) {
        this.tradingClient = tradingClient;
        this.marketDataClient = marketDataClient;
    }

    /** Devuelve hasta {@code topN} símbolos cripto (solo pares USD, sin stablecoins), ordenados de mayor a menor volumen de 24hs. */
    public List<String> fetchTopByVolume(int topN) {
        List<String> allSymbols = tradingClient.getActiveCryptoSymbols().stream()
                .filter(this::isUsdPair)
                .filter(symbol -> !isStablecoin(symbol))
                .toList();

        List<SymbolVolume> volumes = new ArrayList<>();
        for (String symbol : allSymbols) {
            try {
                List<Bar> bars = marketDataClient.getCryptoBars(symbol, 1);
                if (!bars.isEmpty()) {
                    volumes.add(new SymbolVolume(symbol, bars.get(bars.size() - 1).volume()));
                }
            } catch (Exception e) {
                System.err.println("[CryptoUniverseProvider] Error trayendo volumen de " + symbol + ": " + e.getMessage());
            }
        }

        return volumes.stream()
                .sorted(Comparator.comparingLong(SymbolVolume::volume).reversed())
                .limit(topN)
                .map(SymbolVolume::symbol)
                .toList();
    }

    /**
     * Solo pares cotizados en USD (ej. "BTC/USD", no "LINK/BTC").
     * Evita mezclar unidades distintas en el mismo ranking — todo el análisis
     * técnico posterior (TAI-10) trabaja con precios en dólares, no fracciones de otra cripto.
     */
    private boolean isUsdPair(String symbol) {
        return symbol.endsWith("/USD");
    }

    /**
     * Un símbolo como "USDC/USD" es stablecoin si CUALQUIERA de las dos partes
     * (base o cotización) es una stablecoin conocida — cubre tanto "USDC/USD"
     * como pares cruzados tipo "GRT/USDC".
     */
    private boolean isStablecoin(String symbol) {
        String[] parts = symbol.split("/");
        for (String part : parts) {
            if (STABLECOINS.contains(part)) {
                return true;
            }
        }
        return false;
    }

    private record SymbolVolume(String symbol, long volume) {
    }
}
