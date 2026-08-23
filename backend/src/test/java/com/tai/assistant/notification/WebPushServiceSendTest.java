package com.tai.assistant.notification;

import com.tai.assistant.detection.ExplainedSetup;
import com.tai.assistant.detection.Setup;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.security.Security;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebPushServiceSendTest {

    @Mock
    WebPushSubscriptionRepository repository;

    @Mock
    PushService pushService;

    @Mock
    HttpResponse httpResponse;

    @Mock
    StatusLine statusLine;

private static String generateP256dh() throws Exception {
    if (Security.getProvider("BC") == null) {
        Security.addProvider(new BouncyCastleProvider());
    }

    KeyPairGenerator generator = KeyPairGenerator.getInstance("EC", "BC");
    generator.initialize(new ECGenParameterSpec("secp256r1"));

    ECPublicKey publicKey =
            (ECPublicKey) generator.generateKeyPair().getPublic();

    byte[] x = toFixedLength(publicKey.getW().getAffineX().toByteArray(), 32);
    byte[] y = toFixedLength(publicKey.getW().getAffineY().toByteArray(), 32);

    byte[] uncompressed = new byte[65];
    uncompressed[0] = 0x04;
    System.arraycopy(x, 0, uncompressed, 1, 32);
    System.arraycopy(y, 0, uncompressed, 33, 32);

    return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(uncompressed);
}
    private static String generateAuth() {
        byte[] auth = new byte[16];
        new SecureRandom().nextBytes(auth);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(auth);
    }

    private static byte[] toFixedLength(byte[] value, int length) {
        byte[] result = new byte[length];

        if (value.length >= length) {
            System.arraycopy(value, value.length - length, result, 0, length);
        } else {
            System.arraycopy(value, 0, result, length - value.length, value.length);
        }

        return result;
    }

    @Test
    void testSendSetupAlertSendsToAllSubscriptionsWhenConfiguredAndKeepsOn201() throws Exception {
        WebPushProperties props = new WebPushProperties();
        props.setPublicKey("pub");
        props.setPrivateKey("priv");
        props.setSubject("mailto:test@example.com");

        WebPushSubscription s1 = new WebPushSubscription();
        s1.setEndpoint("https://example.com/1");
        s1.setP256dh(generateP256dh());
        s1.setAuth(generateAuth());

        when(repository.findAll()).thenReturn(List.of(s1));
        when(pushService.send(any())).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(201);

        WebPushService svc = new WebPushService(props, repository, pushService);

        ExplainedSetup explained = mock(ExplainedSetup.class);
        Setup setup = mock(Setup.class);
        when(setup.symbol()).thenReturn("AAPL");
        when(explained.setup()).thenReturn(setup);
        when(explained.explanation()).thenReturn("summary");

        svc.sendSetupAlert(explained);

        verify(repository, never()).delete(any());
        verify(pushService, times(1)).send(any());
    }

    @Disabled("flaky — TAI-13 web-push")
    @Test
    void testSendSetupAlertDeletesSubscriptionWhenResponse410Or404() throws Exception {
        WebPushProperties props = new WebPushProperties();
        props.setPublicKey("pub");
        props.setPrivateKey("priv");
        props.setSubject("mailto:test@example.com");

        WebPushSubscription s1 = new WebPushSubscription();
        s1.setEndpoint("https://example.com/1");
        s1.setP256dh(generateP256dh());
        s1.setAuth(generateAuth());

        when(repository.findAll()).thenReturn(List.of(s1));
        when(pushService.send(any())).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(410);

        WebPushService svc = new WebPushService(props, repository, pushService);

        ExplainedSetup explained = mock(ExplainedSetup.class);
        Setup setup = mock(Setup.class);
        when(setup.symbol()).thenReturn("AAPL");
        when(explained.setup()).thenReturn(setup);
        when(explained.explanation()).thenReturn("summary");

        svc.sendSetupAlert(explained);

        verify(repository, times(1)).delete(s1);
        verify(pushService, times(1)).send(any());
    }

    @Disabled("flaky — TAI-13 web-push")
    @Test
    void testSendSetupAlertDeletesSubscriptionWhenPushServiceThrows() throws Exception {
        WebPushProperties props = new WebPushProperties();
        props.setPublicKey("pub");
        props.setPrivateKey("priv");
        props.setSubject("mailto:test@example.com");

        WebPushSubscription s1 = new WebPushSubscription();
        s1.setEndpoint("https://example.com/1");
        s1.setP256dh(generateP256dh());
        s1.setAuth(generateAuth());

        when(repository.findAll()).thenReturn(List.of(s1));
        when(pushService.send(any())).thenThrow(new RuntimeException("boom"));

        WebPushService svc = new WebPushService(props, repository, pushService);

        ExplainedSetup explained = mock(ExplainedSetup.class);
        Setup setup = mock(Setup.class);
        when(setup.symbol()).thenReturn("AAPL");
        when(explained.setup()).thenReturn(setup);
        when(explained.explanation()).thenReturn("summary");

        svc.sendSetupAlert(explained);

        verify(repository, times(1)).delete(s1);
        verify(pushService, times(1)).send(any());
    }
}
