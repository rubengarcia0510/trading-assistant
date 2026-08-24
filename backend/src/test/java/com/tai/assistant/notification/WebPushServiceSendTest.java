package com.tai.assistant.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tai.assistant.detection.ExplainedSetup;
import com.tai.assistant.detection.Setup;
import nl.martijndwars.webpush.Notification;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebPushServiceSendTest {

    @Mock
    WebPushProperties properties;

    @Mock
    WebPushSubscriptionRepository repository;

    @Mock
    ObjectMapper mapper;

    @Mock
    WebPushClient webPushClient;

    @Mock
    Optional<WebPushClient> webPushClientOptional;

    @Mock
    HttpResponse httpResponse;

    @Mock
    StatusLine statusLine;

    @Mock
    WebPushSubscription subscription;

    @InjectMocks
    WebPushService webPushService;

    @Test
    void testSendSetupAlertSendsToAllSubscriptionsWhenConfiguredAndKeepsOn201() throws Exception {
        when(properties.getPublicKey()).thenReturn("pub");
        when(properties.getPrivateKey()).thenReturn("priv");
        when(webPushClientOptional.isPresent()).thenReturn(true);
        when(webPushClientOptional.get()).thenReturn(webPushClient);

        when(subscription.getEndpoint()).thenReturn("https://example.com/1");
        when(subscription.getP256dh()).thenReturn("p256dh_value");
        when(subscription.getAuth()).thenReturn("auth_value");

        when(repository.findAll()).thenReturn(List.of(subscription));
        when(mapper.writeValueAsString(any())).thenReturn("{\"title\":\"test\"}");
        when(webPushClient.send(any())).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(201);

        ExplainedSetup explained = mock(ExplainedSetup.class);
        Setup setup = mock(Setup.class);
        when(setup.symbol()).thenReturn("AAPL");
        when(explained.setup()).thenReturn(setup);
        when(explained.explanation()).thenReturn("summary");

        try (MockedConstruction<Notification> ignored =
                     mockConstruction(Notification.class)) {

            webPushService.sendSetupAlert(explained);
        }

        verify(repository, never()).delete(any());
        verify(webPushClient, times(1)).send(any());
    }

@Test
void testSendSetupAlertRemovesSubscriptionOn404() throws Exception {
    when(properties.getPublicKey()).thenReturn("pub");
    when(properties.getPrivateKey()).thenReturn("priv");
    when(webPushClientOptional.isPresent()).thenReturn(true);
    when(webPushClientOptional.get()).thenReturn(webPushClient);

    when(subscription.getEndpoint()).thenReturn("https://example.com/1");
    when(subscription.getP256dh()).thenReturn("p256dh_value");
    when(subscription.getAuth()).thenReturn("auth_value");

    when(repository.findAll()).thenReturn(List.of(subscription));
    when(mapper.writeValueAsString(any())).thenReturn("{\"title\":\"test\"}");
    when(webPushClient.send(any())).thenReturn(httpResponse);
    when(httpResponse.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(404);

    ExplainedSetup explained = mock(ExplainedSetup.class);
    Setup setup = mock(Setup.class);
    when(setup.symbol()).thenReturn("AAPL");
    when(explained.setup()).thenReturn(setup);
    when(explained.explanation()).thenReturn("summary");

    try (MockedConstruction<Notification> ignored =
                 mockConstruction(Notification.class)) {

        webPushService.sendSetupAlert(explained);
    }

    verify(webPushClient, times(1)).send(any());
    verify(repository, times(1)).delete(subscription);
}

@Test
void testSendSetupAlertRemovesSubscriptionOn410() throws Exception {
    when(properties.getPublicKey()).thenReturn("pub");
    when(properties.getPrivateKey()).thenReturn("priv");
    when(webPushClientOptional.isPresent()).thenReturn(true);
    when(webPushClientOptional.get()).thenReturn(webPushClient);

    when(subscription.getEndpoint()).thenReturn("https://example.com/1");
    when(subscription.getP256dh()).thenReturn("p256dh_value");
    when(subscription.getAuth()).thenReturn("auth_value");

    when(repository.findAll()).thenReturn(List.of(subscription));
    when(mapper.writeValueAsString(any())).thenReturn("{\"title\":\"test\"}");
    when(webPushClient.send(any())).thenReturn(httpResponse);
    when(httpResponse.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(410);

    ExplainedSetup explained = mock(ExplainedSetup.class);
    Setup setup = mock(Setup.class);
    when(setup.symbol()).thenReturn("AAPL");
    when(explained.setup()).thenReturn(setup);
    when(explained.explanation()).thenReturn("summary");

    try (MockedConstruction<Notification> ignored =
                 mockConstruction(Notification.class)) {

        webPushService.sendSetupAlert(explained);
    }

    verify(webPushClient, times(1)).send(any());
    verify(repository, times(1)).delete(subscription);
}

@Test
void testSendSetupAlertKeepsSubscriptionWhenClientThrows() throws Exception {
    when(properties.getPublicKey()).thenReturn("pub");
    when(properties.getPrivateKey()).thenReturn("priv");
    when(webPushClientOptional.isPresent()).thenReturn(true);
    when(webPushClientOptional.get()).thenReturn(webPushClient);

    when(subscription.getEndpoint()).thenReturn("https://example.com/1");
    when(subscription.getP256dh()).thenReturn("p256dh_value");
    when(subscription.getAuth()).thenReturn("auth_value");

    when(repository.findAll()).thenReturn(List.of(subscription));
    when(mapper.writeValueAsString(any())).thenReturn("{\"title\":\"test\"}");
    when(webPushClient.send(any()))
            .thenThrow(new RuntimeException("push failed"));

    ExplainedSetup explained = mock(ExplainedSetup.class);
    Setup setup = mock(Setup.class);
    when(setup.symbol()).thenReturn("AAPL");
    when(explained.setup()).thenReturn(setup);
    when(explained.explanation()).thenReturn("summary");

    try (MockedConstruction<Notification> ignored =
                 mockConstruction(Notification.class)) {

        webPushService.sendSetupAlert(explained);
    }

    verify(webPushClient, times(1)).send(any());
    verify(repository, never()).delete(any());
}

}
