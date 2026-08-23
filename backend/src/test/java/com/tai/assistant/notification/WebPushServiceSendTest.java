package com.tai.assistant.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tai.assistant.detection.ExplainedSetup;
import com.tai.assistant.detection.Setup;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebPushServiceSendTest {

    @Mock
    WebPushSubscriptionRepository repository;

    @Mock
    WebPushClient webPushClient;

    @Mock
    HttpResponse httpResponse;

    @Mock
    StatusLine statusLine;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testSendSetupAlertSendsToAllSubscriptionsWhenConfiguredAndKeepsOn201() throws Exception {
        WebPushProperties props = new WebPushProperties();
        props.setPublicKey("pub");
        props.setPrivateKey("priv");
        props.setSubject("mailto:test@example.com");

        WebPushSubscription s1 = new WebPushSubscription();
        s1.setEndpoint("https://example.com/1");
        s1.setP256dh("p256dh_value");
        s1.setAuth("auth_value");

        when(repository.findAll()).thenReturn(List.of(s1));
        when(webPushClient.send(any())).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(201);

        WebPushService svc = new WebPushService(props, repository, mapper, Optional.of(webPushClient));

        ExplainedSetup explained = mock(ExplainedSetup.class);
        Setup setup = mock(Setup.class);
        when(setup.symbol()).thenReturn("AAPL");
        when(explained.setup()).thenReturn(setup);
        when(explained.explanation()).thenReturn("summary");

        svc.sendSetupAlert(explained);

        verify(repository, never()).delete(any());
        verify(webPushClient, times(1)).send(any());
    }

    @Test
    void testSendSetupAlertDeletesSubscriptionWhenResponse404() throws Exception {
        WebPushProperties props = new WebPushProperties();
        props.setPublicKey("pub");
        props.setPrivateKey("priv");
        props.setSubject("mailto:test@example.com");

        WebPushSubscription s1 = new WebPushSubscription();
        s1.setEndpoint("https://example.com/1");
        s1.setP256dh("p256dh_value");
        s1.setAuth("auth_value");

        when(repository.findAll()).thenReturn(List.of(s1));
        when(webPushClient.send(any())).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(404);

        WebPushService svc = new WebPushService(props, repository, mapper, Optional.of(webPushClient));

        ExplainedSetup explained = mock(ExplainedSetup.class);
        Setup setup = mock(Setup.class);
        when(setup.symbol()).thenReturn("AAPL");
        when(explained.setup()).thenReturn(setup);
        when(explained.explanation()).thenReturn("summary");

        svc.sendSetupAlert(explained);

        verify(repository, times(1)).delete(s1);
        verify(webPushClient, times(1)).send(any());
    }

    @Test
    void testSendSetupAlertDeletesSubscriptionWhenResponse410() throws Exception {
        WebPushProperties props = new WebPushProperties();
        props.setPublicKey("pub");
        props.setPrivateKey("priv");
        props.setSubject("mailto:test@example.com");

        WebPushSubscription s1 = new WebPushSubscription();
        s1.setEndpoint("https://example.com/1");
        s1.setP256dh("p256dh_value");
        s1.setAuth("auth_value");

        when(repository.findAll()).thenReturn(List.of(s1));
        when(webPushClient.send(any())).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(410);

        WebPushService svc = new WebPushService(props, repository, mapper, Optional.of(webPushClient));

        ExplainedSetup explained = mock(ExplainedSetup.class);
        Setup setup = mock(Setup.class);
        when(setup.symbol()).thenReturn("AAPL");
        when(explained.setup()).thenReturn(setup);
        when(explained.explanation()).thenReturn("summary");

        svc.sendSetupAlert(explained);

        verify(repository, times(1)).delete(s1);
        verify(webPushClient, times(1)).send(any());
    }

    @Test
    void testSendSetupAlertDoesNotDeleteSubscriptionWhenWebPushClientThrows() throws Exception {
        WebPushProperties props = new WebPushProperties();
        props.setPublicKey("pub");
        props.setPrivateKey("priv");
        props.setSubject("mailto:test@example.com");

        WebPushSubscription s1 = new WebPushSubscription();
        s1.setEndpoint("https://example.com/1");
        s1.setP256dh("p256dh_value");
        s1.setAuth("auth_value");

        when(repository.findAll()).thenReturn(List.of(s1));
        when(webPushClient.send(any())).thenThrow(new RuntimeException("boom"));

        WebPushService svc = new WebPushService(props, repository, mapper, Optional.of(webPushClient));

        ExplainedSetup explained = mock(ExplainedSetup.class);
        Setup setup = mock(Setup.class);
        when(setup.symbol()).thenReturn("AAPL");
        when(explained.setup()).thenReturn(setup);
        when(explained.explanation()).thenReturn("summary");

        svc.sendSetupAlert(explained);

        verify(repository, never()).delete(any());
        verify(webPushClient, times(1)).send(any());
    }

    @Test
    void testSendSetupAlertSkipsWhenNotConfigured() throws Exception {
        WebPushProperties props = new WebPushProperties();
        // No keys set -> not configured
        
        WebPushService svc = new WebPushService(props, repository, mapper, Optional.of(webPushClient));

        ExplainedSetup explained = mock(ExplainedSetup.class);

        svc.sendSetupAlert(explained);

        verify(repository, never()).findAll();
        verify(webPushClient, never()).send(any());
    }
}
