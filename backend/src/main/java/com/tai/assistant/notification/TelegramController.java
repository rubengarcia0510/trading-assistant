package com.tai.assistant.notification;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** GET /telegram/decisions -> decisiones recientes (aprobado/descartado) tomadas desde el bot. */
@RestController
@RequestMapping("/telegram")
public class TelegramController {

    private final TelegramCallbackPoller poller;

    public TelegramController(TelegramCallbackPoller poller) {
        this.poller = poller;
    }

    @GetMapping("/decisions")
    public List<SetupDecision> decisions() {
        return poller.getRecentDecisions();
    }
}
