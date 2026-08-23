package com.tai.assistant.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tai.assistant.detection.ExplainedSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebPushServiceTest {

    @Mock
    WebPushSubscriptionRepository repository;

    @Mock
    ObjectMapper mapper;

    @Test
    void testSendSetupAlertSkipsWhenNotConfigured() {
        // WebPushProperties sin claves -> isConfigured() == false
        WebPushProperties props = new WebPushProperties();
        WebPushService svc = new WebPushService(props, repository, mapper, Optional.empty());

        ExplainedSetup explained = mock(ExplainedSetup.class);

        // No debe lanzar excepción y no debe llamar a repository.findAll()
        svc.sendSetupAlert(explained);

        verify(repository, never()).findAll();
    }

    @Test
    void testSendSetupAlertSkipsWhenWebPushClientNotPresent() {
        // WebPushProperties con claves pero sin WebPushClient -> isConfigured() == false
        WebPushProperties props = new WebPushProperties();
        props.setPublicKey("pub");
        props.setPrivateKey("priv");
        WebPushService svc = new WebPushService(props, repository, mapper, Optional.empty());

        ExplainedSetup explained = mock(ExplainedSetup.class);

        // No debe intentar enviar
        svc.sendSetupAlert(explained);

        verify(repository, never()).findAll();
    }
}
