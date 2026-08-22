package com.tai.assistant.history;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface HistoryEntryRepository extends MongoRepository<HistoryEntry, String> {

    /** Última entrada PENDIENTE (decision null) de un símbolo — para completarla al decidir. */
    Optional<HistoryEntry> findFirstBySymbolAndDecisionIsNullOrderByDetectedAtDesc(String symbol);

    List<HistoryEntry> findByDecisionIsNotNullOrderByDecidedAtDesc();
}
