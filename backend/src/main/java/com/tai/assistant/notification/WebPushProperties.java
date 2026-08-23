package com.tai.assistant.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Web Push (VAPID)
 */
@ConfigurationProperties(prefix = "tai.webpush")
public class WebPushProperties {

    /** VAPID public key (Base64 URL-safe) exposed to clients */
    private String publicKey;

    /** VAPID private key (keep secret; do not commit) */
    private String privateKey;

    /** VAPID subject (mailto: or URL) */
    private String subject;

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }
}
