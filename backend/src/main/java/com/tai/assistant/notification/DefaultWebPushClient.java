package com.tai.assistant.notification;

import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.GeneralSecurityException;
import java.security.Security;

/**
 * Implementación real de WebPushClient.
 * Encapsula la dependencia de nl.martijndwars.webpush.PushService.
 * Responsable de:
 * - Crear y utilizar PushService
 * - Registrar BouncyCastle provider
 * - Enviar notificaciones
 */
public class DefaultWebPushClient implements WebPushClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultWebPushClient.class);

    private final PushService pushService;

    public DefaultWebPushClient(String publicKey, String privateKey, String subject)
            throws GeneralSecurityException {
        ensureBouncyCastleProvider();
        this.pushService = new PushService(publicKey, privateKey, subject);
    }

    @Override
    public HttpResponse send(Notification notification) throws Exception {
        return pushService.send(notification);
    }

    private static void ensureBouncyCastleProvider() {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }
}
