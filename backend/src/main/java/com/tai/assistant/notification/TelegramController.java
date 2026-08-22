package com.tai.assistant.notification;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/telegram")
public class TelegramController {

    private final DecisionStore decisionStore;

    public TelegramController(DecisionStore decisionStore) {
        this.decisionStore = decisionStore;
    }

    @GetMapping("/decisions")
    public List<SetupDecision> decisions() {
        return decisionStore.getRecent();
    }
}
