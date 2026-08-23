package com.tai.assistant.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import com.tai.assistant.notification.WebPushProperties;

@Configuration
@EnableConfigurationProperties({
        AlpacaProperties.class,
        FinnhubProperties.class,
        TelegramProperties.class,
        AuthProperties.class,
        JwtProperties.class,
        WebPushProperties.class
})
public class AppConfig {

    @Bean
    public RestClient restClient() {
        return RestClient.create();
    }
}
