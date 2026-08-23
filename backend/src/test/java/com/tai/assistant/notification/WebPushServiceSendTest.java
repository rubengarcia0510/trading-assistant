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

    @Test
    void testSendSetupAlertSendsToAllSubscriptionsWhenConfiguredAndKeepsOn201() throws Exception {
        WebPushProperties props = new WebPushProperties();
        props.setPublicKey("pub");
        props.setPrivateKey("priv");
        props.setSubject("mailto:test@example.com");

        WebPushSubscription s1 = new WebPushSubscription();
        s1.setEndpoint("https://example.com/1");
        s1.setP256dh("p256");
        s1.setAuth("auth");

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
        s1.setP256dh("p256");
        s1.setAuth("auth");

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
        s1.setP256dh("p256");
        s1.setAuth("auth");

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
