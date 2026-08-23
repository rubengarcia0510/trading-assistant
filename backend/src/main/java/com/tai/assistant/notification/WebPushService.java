package com.tai.assistant.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tai.assistant.detection.ExplainedSetup;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.Subscription;
import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class WebPushService {

    private static final Logger log = LoggerFactory.getLogger(WebPushService.class);

    private final WebPushProperties properties;
    private final WebPushSubscriptionRepository repository;
    private final ObjectMapper mapper;
    private final Optional<WebPushClient> webPushClient;

    public WebPushService(WebPushProperties properties,
                         WebPushSubscriptionRepository repository,
                         ObjectMapper mapper,
                         Optional<WebPushClient> webPushClient) {
        this.properties = properties;
        this.repository = repository;
        this.mapper = mapper;
        this.webPushClient = webPushClient;
    }

    public boolean isConfigured() {
        return properties.getPublicKey() != null && !properties.getPublicKey().isBlank()
                && properties.getPrivateKey() != null && !properties.getPrivateKey().isBlank()
                && webPushClient.isPresent();
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

                HttpResponse response = webPushClient.get().send(notification);
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
