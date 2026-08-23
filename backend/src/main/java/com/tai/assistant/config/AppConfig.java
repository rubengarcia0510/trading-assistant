package com.tai.assistant.config;

import com.tai.assistant.notification.DefaultWebPushClient;
import com.tai.assistant.notification.WebPushClient;
import com.tai.assistant.notification.WebPushProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.security.GeneralSecurityException;

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

    @Bean
    @ConditionalOnProperty(
            prefix = "tai.webpush",
            name = {"public-key", "private-key"},
            matchIfMissing = false
    )
    public WebPushClient webPushClient(WebPushProperties properties) throws GeneralSecurityException {
        return new DefaultWebPushClient(
                properties.getPublicKey(),
                properties.getPrivateKey(),
                properties.getSubject()
        );
    }
}
