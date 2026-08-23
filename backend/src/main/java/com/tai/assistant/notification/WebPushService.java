package com.tai.assistant.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import nl.martijndwars.webpush.Utils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.http.HttpResponse;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.time.Instant;
import java.util.List;

@Service
public class WebPushService {

    private static final Logger log = LoggerFactory.getLogger(WebPushService.class);

    private final WebPushProperties properties;
    private final WebPushSubscriptionRepository repository;
    private final ObjectMapper mapper = new ObjectMapper();
    private final PushService pushService;

    public WebPushService(WebPushProperties properties, WebPushSubscriptionRepository repository) throws GeneralSecurityException {
        this.properties = properties;
        this.repository = repository;
        Security.addProvider(new BouncyCastleProvider());

        if (isConfigured()) {
            this.pushService = new PushService(
                    Utils.loadVapidPrivateKey(properties.getPrivateKey()),
                    properties.getPublicKey(),
                    properties.getSubject());
        } else {
            this.pushService = null;
        }
    }

    public boolean isConfigured() {
        return properties.getPublicKey() != null && !properties.getPublicKey().isBlank()
                && properties.getPrivateKey() != null && !properties.getPrivateKey().isBlank();
    }

    public void sendSetupAlert(ExplainedSetup explained) {
        if (!isConfigured()) {
            log.info("WebPush not configured; skipping push send");
            return;
        }

        List<WebPushSubscription> subs = repository.findAll();
        for (WebPushSubscription s : subs) {
            try {
                String payload = mapper.writeValueAsString(new PushPayload(explained));
                Subscription sub = new Subscription(s.getEndpoint(), new Subscription.Keys(s.getP256dh(), s.getAuth()));
                Notification notification = new Notification(sub, payload.getBytes());
                java.net.http.HttpResponse<byte[]> resp = pushService.send(notification);
                int status = resp.statusCode();
                if (status == 201 || status == 200) {
                    log.info("Sent web push to {}", s.getEndpoint());
                } else if (status == 404 || status == 410) {
                    log.warn("Subscription gone ({}), removing", s.getEndpoint());
                    repository.delete(s);
                } else {
                    log.warn("Unexpected status {} when sending to {}", status, s.getEndpoint());
                }
            } catch (Exception e) {
                log.error("Error sending web push to {}: {}", s.getEndpoint(), e.getMessage());
                // If the error indicates invalid subscription, remove it
                repository.delete(s);
            }
        }
    }

    // Simple payload object
    static class PushPayload {
        public String title;
        public String body;
        public String symbol;
        public String url;

        PushPayload(ExplainedSetup explained) {
            this.title = "TAI — Setup detectado: " + explained.setup().symbol();
            this.body = explained.summary();
            this.symbol = explained.setup().symbol();
            this.url = "/setups";
        }
    }
}
