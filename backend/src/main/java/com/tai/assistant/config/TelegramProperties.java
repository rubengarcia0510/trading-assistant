package com.tai.assistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tai.telegram")
public class TelegramProperties {

    private String botToken;
    private String chatId;

    public String getBotToken() { return botToken; }
    public void setBotToken(String botToken) { this.botToken = botToken; }

    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public boolean isConfigured() {
        return botToken != null && !botToken.isBlank() && chatId != null && !chatId.isBlank();
    }
}
