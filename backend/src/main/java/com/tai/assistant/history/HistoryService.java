package com.tai.assistant.history;

import com.tai.assistant.detection.ExplainedSetup;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Única fuente de verdad para el historial de setups + decisiones (TAI-15).
 * Reemplaza al DecisionStore en memoria de TAI-14.
 */
@Service
public class HistoryService {

    private final HistoryEntryRepository repository;
    private final MongoTemplate mongoTemplate;

    public HistoryService(HistoryEntryRepository repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    /** Se llama cuando SetupDetectionService encuentra un setup — lo guarda como PENDIENTE. */
    public void recordDetection(ExplainedSetup explained) {
        var setup = explained.setup();
        HistoryEntry entry = new HistoryEntry(
                setup.symbol(), setup.assetType(), setup.entryPrice(), setup.stopLoss(),
                setup.takeProfit(), setup.riskLevel(), explained.explanation(),
                setup.recentCloses(), setup.detectedAt()
        );
        repository.save(entry);
    }

    /**
     * Se llama al aprobar/descartar (desde web o Telegram) — busca la entrada
     * pendiente más reciente de ese símbolo y la completa con la decisión.
     */
    public void recordDecision(String symbol, String decision, String source) {
        repository.findFirstBySymbolAndDecisionIsNullOrderByDetectedAtDesc(symbol).ifPresent(entry -> {
            entry.setDecision(decision);
            entry.setDecidedAt(Instant.now());
            entry.setSource(source);
            repository.save(entry);
        });
    }

    /** Entradas decididas recientes, sin filtros — para el resumen del dashboard (TAI-14). */
    public List<HistoryEntry> recentDecided(int limit) {
        return repository.findByDecisionIsNotNullOrderByDecidedAtDesc().stream().limit(limit).toList();
    }

    /**
     * Búsqueda con filtros combinables (símbolo, rango de fechas) y paginación —
     * el listado histórico completo de TAI-15. Todos los filtros son opcionales.
     */
    public PageResult<HistoryEntry> search(String symbol, Instant from, Instant to, int page, int size) {
        Criteria criteria = Criteria.where("decision").ne(null);

        if (symbol != null && !symbol.isBlank()) {
            criteria = criteria.and("symbol").is(symbol.toUpperCase());
        }
        if (from != null || to != null) {
            Criteria dateCriteria = Criteria.where("decidedAt");
            if (from != null) dateCriteria = dateCriteria.gte(from);
            if (to != null) dateCriteria = dateCriteria.lte(to);
            criteria = criteria.andOperator(dateCriteria);
        }

        Query query = new Query(criteria)
                .with(PageRequest.of(page, size))
                .with(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "decidedAt"));

        List<HistoryEntry> content = mongoTemplate.find(query, HistoryEntry.class);
        long total = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), HistoryEntry.class);

        return new PageResult<>(content, total, page, size);
    }
}
