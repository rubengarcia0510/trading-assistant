package com.tai.assistant.notification;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/notification/push")
public class WebPushController {

    private final WebPushSubscriptionRepository repository;
    private final WebPushProperties properties;

    public WebPushController(WebPushSubscriptionRepository repository, WebPushProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    record SubscriptionRequest(String endpoint, Map<String, String> keys) {}

    @PostMapping("/subscribe")
    public ResponseEntity<?> subscribe(@RequestBody SubscriptionRequest req,
                                       @AuthenticationPrincipal(expression = "username") Object principal) {
        if (req == null || req.endpoint == null || req.endpoint.isBlank() || req.keys == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid subscription payload"));
        }

        String p256dh = req.keys.get("p256dh");
        String auth = req.keys.get("auth");
        if (p256dh == null || auth == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "missing keys"));
        }

        Optional<WebPushSubscription> existing = repository.findByEndpoint(req.endpoint);
        WebPushSubscription sub = existing.orElseGet(WebPushSubscription::new);
        sub.setEndpoint(req.endpoint);
        sub.setP256dh(p256dh);
        sub.setAuth(auth);
        if (principal != null) {
            sub.setOwner(principal.toString());
        }
        sub.setUpdatedAt(Instant.now());
        if (sub.getCreatedAt() == null) {
            sub.setCreatedAt(Instant.now());
        }

        repository.save(sub);
        return ResponseEntity.ok(Map.of("status", "subscribed"));
    }

    @DeleteMapping("/subscribe")
    public ResponseEntity<?> unsubscribe(@RequestParam(required = false) String endpoint,
                                         @RequestBody(required = false) Map<String, Object> body) {
        String toDelete = endpoint;
        if ((toDelete == null || toDelete.isBlank()) && body != null) {
            Object e = body.get("endpoint");
            if (e instanceof String) toDelete = (String) e;
        }
        if (toDelete == null || toDelete.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "endpoint is required"));
        }

        if (repository.existsByEndpoint(toDelete)) {
            repository.deleteByEndpoint(toDelete);
            return ResponseEntity.ok(Map.of("status", "unsubscribed"));
        } else {
            return ResponseEntity.status(404).body(Map.of("error", "not_found"));
        }
    }

    @GetMapping("/public-key")
    public ResponseEntity<?> publicKey() {
        String key = properties.getPublicKey();
        if (key == null || key.isBlank()) {
            return ResponseEntity.status(404).body(Map.of("error", "public_key_not_configured"));
        }
        return ResponseEntity.ok(Map.of("publicKey", key));
    }
}
