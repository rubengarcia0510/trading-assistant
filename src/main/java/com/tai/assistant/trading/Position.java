package com.tai.assistant.trading;

public record Position(String symbol, double qty, double avgEntryPrice, double currentPrice, double unrealizedPl) {
}
