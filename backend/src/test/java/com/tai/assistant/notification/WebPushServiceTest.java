package com.tai.assistant.notification;

import com.tai.assistant.detection.ExplainedSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.GeneralSecurityException;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebPushServiceTest {

    @Mock
    WebPushSubscriptionRepository repository;

    @Test
    void sendSetupAlert_skipsWhenNotConfigured() throws GeneralSecurityException {
        // WebPushProperties without keys -> isConfigured() == false
        WebPushProperties props = new WebPushProperties();
        WebPushService svc = new WebPushService(props, repository);

        ExplainedSetup explained = mock(ExplainedSetup.class);

        // Should not throw and must not call repository.findAll()
        svc.sendSetupAlert(explained);

        verify(repository, never()).findAll();
    }
}
