package com.tai.assistant.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tai.assistant.detection.ExplainedSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebPushServiceTest {

    @Mock
    WebPushProperties properties;

    @Mock
    WebPushSubscriptionRepository repository;

    @Mock
    ObjectMapper mapper;

    @Mock
    Optional<WebPushClient> webPushClient;

    @InjectMocks
    WebPushService webPushService;

    @Test
    void testSendSetupAlertSkipsWhenNotConfigured() {
        when(properties.getPublicKey()).thenReturn(null);

        ExplainedSetup explained = mock(ExplainedSetup.class);

        webPushService.sendSetupAlert(explained);

        verify(repository, never()).findAll();
        verify(webPushClient, never()).get();
    }

    @Test
    void testSendSetupAlertSkipsWhenWebPushClientNotPresent() {
        when(properties.getPublicKey()).thenReturn("pub");
        when(properties.getPrivateKey()).thenReturn("priv");
        when(webPushClient.isPresent()).thenReturn(false);

        ExplainedSetup explained = mock(ExplainedSetup.class);

        webPushService.sendSetupAlert(explained);

        verify(repository, never()).findAll();
        verify(webPushClient, never()).get();
    }
}
