package com.tai.assistant.market;

/** Barra OHLCV (open/high/low/close/volume) de un período. */
public record Bar(String timestamp, double open, double high, double low, double close, long volume) {
}
