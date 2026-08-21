package com.tai.assistant.trading;

public record Account(String id, String status, double cash, double portfolioValue, boolean tradingBlocked) {
}
