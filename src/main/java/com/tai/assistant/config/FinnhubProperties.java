package com.tai.assistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Finnhub — fuente de capitalización de mercado (reemplaza a FMP, que jubiló
 * el endpoint de screener del plan gratuito). Free tier: 60 requests/minuto, sin tarjeta.
 */
@ConfigurationProperties(prefix = "tai.finnhub")
public class FinnhubProperties {

    private String apiKey;
    private String baseUrl;

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
