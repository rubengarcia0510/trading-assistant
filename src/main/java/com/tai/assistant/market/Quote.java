package com.tai.assistant.market;

/**
 * Último precio de un símbolo. "bidPrice"/"askPrice" son los de compra/venta;
 * para el análisis técnico solemos usar el punto medio o el último trade, según el endpoint.
 */
public record Quote(String symbol, double bidPrice, double askPrice, String timestamp) {

    public double midPrice() {
        return (bidPrice + askPrice) / 2.0;
    }
}
