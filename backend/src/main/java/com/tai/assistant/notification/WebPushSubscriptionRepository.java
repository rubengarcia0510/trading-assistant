package com.tai.assistant.notification;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WebPushSubscriptionRepository extends MongoRepository<WebPushSubscription, String> {

    Optional<WebPushSubscription> findByEndpoint(String endpoint);

    void deleteByEndpoint(String endpoint);

    boolean existsByEndpoint(String endpoint);
}
