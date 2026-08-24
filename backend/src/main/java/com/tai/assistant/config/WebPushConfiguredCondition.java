package com.tai.assistant.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class WebPushConfiguredCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String publicKey = context.getEnvironment().getProperty("tai.webpush.public-key");
        String privateKey = context.getEnvironment().getProperty("tai.webpush.private-key");

        return publicKey != null && !publicKey.isBlank()
                && privateKey != null && !privateKey.isBlank();
    }
}
