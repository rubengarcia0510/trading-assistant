package com.tai.assistant.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tai.assistant.detection.ExplainedSetup;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.List;

@Service
public class WebPushService {

    private static final Logger log = LoggerFactory.getLogger(WebPushService.class);

    private final WebPushProperties properties;
    private final WebPushSubscriptionRepository repository;
    private final ObjectMapper mapper = new ObjectMapper();
    private final PushService pushService;

    public WebPushService(WebPushProperties properties, WebPushSubscriptionRepository repository) throws GeneralSecurityException {
        this(properties, repository, buildPushService(properties));
    }

    public WebPushService(WebPushProperties properties, WebPushSubscriptionRepository repository, PushService pushService) {
        ensureBouncyCastleProvider();
        this.properties = properties;
        this.repository = repository;
        this.pushService = pushService;
    }

    private static void ensureBouncyCastleProvider() {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private static PushService buildPushService(WebPushProperties properties) throws GeneralSecurityException {
        ensureBouncyCastleProvider();

        if (properties.getPublicKey() == null || properties.getPublicKey().isBlank()
                || properties.getPrivateKey() == null || properties.getPrivateKey().isBlank()) {
            return null;
        }

        return new PushService(properties.getPublicKey(), properties.getPrivateKey(), properties.getSubject());
    }

    public boolean isConfigured() {
        return properties.getPublicKey() != null && !properties.getPublicKey().isBlank()
                && properties.getPrivateKey() != null && !properties.getPrivateKey().isBlank();
    }

    public void sendSetupAlert(ExplainedSetup explained) {
        if (!isConfigured()) {
            log.info("WebPush no configurado; se omite el envío.");
            return;
        }

        List<WebPushSubscription> subs = repository.findAll();
        for (WebPushSubscription s : subs) {
            try {
                String payload = mapper.writeValueAsString(new PushPayload(explained));
                Subscription subscription = new Subscription(s.getEndpoint(), new Subscription.Keys(s.getP256dh(), s.getAuth()));
                Notification notification = new Notification(subscription, payload);

                HttpResponse response = pushService.send(notification);
                int status = response.getStatusLine().getStatusCode();

                if (status == 200 || status == 201) {
                    log.info("Web push enviado a {}", s.getEndpoint());
                } else if (status == 404 || status == 410) {
                    log.warn("Suscripción vencida ({}), se elimina", s.getEndpoint());
                    repository.delete(s);
                } else {
                    log.warn("Status inesperado {} al enviar a {}", status, s.getEndpoint());
                }
            } catch (Exception e) {
                log.error("Error enviando web push a {}: {}", s.getEndpoint(), e.getMessage());
            }
        }
    }

    static class PushPayload {
        public String title;
        public String body;
        public String symbol;
        public String url;

        PushPayload(ExplainedSetup explained) {
            this.title = "TAI — Setup detectado: " + explained.setup().symbol();
            this.body = explained.explanation();
            this.symbol = explained.setup().symbol();
            this.url = "/setups";
        }
    }
}
