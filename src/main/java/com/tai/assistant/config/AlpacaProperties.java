package com.tai.assistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración de Alpaca. Alpaca separa datos de mercado (data.alpaca.markets)
 * de trading (paper-api.alpaca.markets para cuenta paper, api.alpaca.markets para real)
 * — por eso hay dos base URLs distintas, aunque las credenciales sean las mismas.
 */
@ConfigurationProperties(prefix = "tai.alpaca")
public class AlpacaProperties {

    private String apiKey;
    private String secretKey;
    private String dataBaseUrl;
    private String tradingBaseUrl;

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

    public String getDataBaseUrl() { return dataBaseUrl; }
    public void setDataBaseUrl(String dataBaseUrl) { this.dataBaseUrl = dataBaseUrl; }

    public String getTradingBaseUrl() { return tradingBaseUrl; }
    public void setTradingBaseUrl(String tradingBaseUrl) { this.tradingBaseUrl = tradingBaseUrl; }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && secretKey != null && !secretKey.isBlank();
    }
}
