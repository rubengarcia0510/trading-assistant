package com.tai.assistant.universe;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface UniverseRepository extends MongoRepository<UniverseDocument, String> {
}
