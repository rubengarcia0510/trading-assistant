package com.tai.assistant.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tai.assistant.detection.ExplainedSetup;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import nl.martijndwars.webpush.Utils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
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
        this(properties, repository, createPushServiceIfConfigured(properties));
    }

    // Test-friendly constructor to inject a mock PushService
    public WebPushService(WebPushProperties properties, WebPushSubscriptionRepository repository, PushService pushService) {
        this.properties = properties;
        this.repository = repository;
        this.pushService = pushService;
    }

    private static PushService createPushServiceIfConfigured(WebPushProperties properties) throws GeneralSecurityException {
        Security.addProvider(new BouncyCastleProvider());
        if (properties.getPublicKey() != null && !properties.getPublicKey().isBlank()
                && properties.getPrivateKey() != null && !properties.getPrivateKey().isBlank()) {
            try {
                // Try multiple Utils methods to load the private key (different versions of web-push)
                PrivateKey privateKey = null;
                String priv = properties.getPrivateKey();
                Class<?> utilsClass = Utils.class;
                String[] methodNames = new String[]{"loadVapidPrivateKey", "loadPrivateKey", "loadEcPrivateKey", "loadPrivateKeyFromString", "fromPem"};
                for (String mName : methodNames) {
                    try {
                        Method m = utilsClass.getMethod(mName, String.class);
                        Object res = m.invoke(null, priv);
                        if (res instanceof PrivateKey) {
                            privateKey = (PrivateKey) res;
                            break;
                        }
                    } catch (NoSuchMethodException ignored) {
                    }
                }

                PushService ps = null;
                if (privateKey != null) {
                    // Try constructor PushService(PrivateKey, String publicKey, String subject)
                    try {
                        Constructor<PushService> cons = PushService.class.getConstructor(PrivateKey.class, String.class, String.class);
                        ps = cons.newInstance(privateKey, properties.getPublicKey(), properties.getSubject());
                    } catch (NoSuchMethodException ex) {
                        // Fall back to default constructor
                        ps = new PushService();
                    }
                } else {
                    // Fall back to constructor taking String keys (older/newer API variations)
                    try {
                        Constructor<PushService> cons = PushService.class.getConstructor(String.class, String.class, String.class);
                        ps = cons.newInstance(properties.getPrivateKey(), properties.getPublicKey(), properties.getSubject());
                    } catch (NoSuchMethodException ex) {
                        ps = new PushService();
                    }
                }
                return ps;
            } catch (Exception e) {
                throw new GeneralSecurityException("Failed to create PushService", e);
            }
        }
        return null;
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

                // Create Notification using whatever constructor exists
                Object notificationObj;
                try {
                    Constructor<Notification> c = Notification.class.getConstructor(Subscription.class, byte[].class);
                    notificationObj = c.newInstance(sub, payload.getBytes());
                } catch (NoSuchMethodException e) {
                    try {
                        Constructor<Notification> c2 = Notification.class.getConstructor(Subscription.class, String.class);
                        notificationObj = c2.newInstance(sub, payload);
                    } catch (NoSuchMethodException ex) {
                        // Last resort: use any available constructor
                        Constructor<?>[] ctors = Notification.class.getConstructors();
                        if (ctors.length > 0) {
                            Constructor<?> any = ctors[0];
                            Class<?>[] params = any.getParameterTypes();
                            if (params.length >= 2 && params[1].isArray() && params[1].getComponentType() == byte.class) {
                                notificationObj = any.newInstance(sub, payload.getBytes());
                            } else if (params.length >= 2) {
                                notificationObj = any.newInstance(sub, payload);
                            } else {
                                // Fallback to a normal constructor that may exist
                                notificationObj = new Notification(sub, payload);
                            }
                        } else {
                            notificationObj = new Notification(sub, payload);
                        }
                    }
                }

                // Invoke pushService.send(...) via reflection to support different return types
                Object resp = pushService.getClass().getMethod("send", Notification.class).invoke(pushService, notificationObj);

                int status = -1;
                if (resp != null) {
                    // Try java.net.http.HttpResponse.statusCode()
                    try {
                        Method m = resp.getClass().getMethod("statusCode");
                        Object sc = m.invoke(resp);
                        if (sc instanceof Integer) status = (Integer) sc;
                    } catch (NoSuchMethodException ignored) {
                    }

                    // Try Apache HttpResponse: getStatusLine().getStatusCode()
                    if (status == -1) {
                        try {
                            Method getStatusLine = resp.getClass().getMethod("getStatusLine");
                            Object statusLine = getStatusLine.invoke(resp);
                            Method getStatusCode = statusLine.getClass().getMethod("getStatusCode");
                            Object sc = getStatusCode.invoke(statusLine);
                            if (sc instanceof Integer) status = (Integer) sc;
                        } catch (NoSuchMethodException ignored) {
                        }
                    }

                    // Try getStatusCode()
                    if (status == -1) {
                        try {
                            Method getStatusCode = resp.getClass().getMethod("getStatusCode");
                            Object sc = getStatusCode.invoke(resp);
                            if (sc instanceof Integer) status = (Integer) sc;
                        } catch (NoSuchMethodException ignored) {
                        }
                    }
                }

                if (status == 201 || status == 200) {
                    log.info("Sent web push to {}", s.getEndpoint());
                } else if (status == 404 || status == 410) {
                    log.warn("Subscription gone ({}), removing", s.getEndpoint());
                    repository.delete(s);
                } else if (status == -1) {
                    log.warn("Could not determine status when sending to {}", s.getEndpoint());
                } else {
                    log.warn("Unexpected status {} when sending to {}", status, s.getEndpoint());
                }
            } catch (Exception e) {
                log.error("Error sending web push to {}: {}", s.getEndpoint(), e.getMessage());
                try {
                    repository.delete(s);
                } catch (Exception ignored) {
                }
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
            this.body = explained.explanation();
            this.symbol = explained.setup().symbol();
            this.url = "/setups";
        }
    }
}
