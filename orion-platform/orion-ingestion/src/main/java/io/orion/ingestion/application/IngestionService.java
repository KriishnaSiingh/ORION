package io.orion.ingestion.application;

import io.orion.ingestion.infrastructure.RawZoneService;
import io.orion.ingestion.infrastructure.parser.CsvParser;
import io.orion.ingestion.infrastructure.event.IngestionEventPublisher;
import io.orion.knowledge.application.KnowledgeService;
import io.orion.knowledge.domain.KnowledgeObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.Instant;
import java.util.*;
import java.util.logging.Logger;

@Service
public class IngestionService {
    private final RawZoneService rawZoneService;
    private final CsvParser csvParser;
    private final KnowledgeService knowledgeService;
    private final IngestionEventPublisher eventPublisher;
    private static final Logger logger = Logger.getLogger(IngestionService.class.getName());

    public IngestionService(RawZoneService rawZoneService, CsvParser csvParser, KnowledgeService knowledgeService, IngestionEventPublisher eventPublisher) {
        this.rawZoneService = rawZoneService;
        this.csvParser = csvParser;
        this.knowledgeService = knowledgeService;
        this.eventPublisher = eventPublisher;
    }

    public List<String> detectColumns(String jobId) {
        File file = rawZoneService.getFile(jobId);
        if (file == null || !file.exists()) {
            throw new RuntimeException("File not found for jobId: " + jobId);
        }

        logger.info("Detecting columns for file: " + file.getName());
        try {
            return csvParser.detectHeaders(file);
        } catch (Exception e) {
            throw new RuntimeException("Failed to detect columns", e);
        }
    }

    @Transactional
    public Map<String, Object> processIngestion(String jobId, String objectType, Map<String, String> mappings) {
        logger.info("Processing ingestion for jobId: " + jobId + " as type: " + objectType);

        File file = rawZoneService.getFile(jobId);
        if (file == null || !file.exists()) {
            throw new RuntimeException("File not found for jobId: " + jobId);
        }

        try {
            List<Map<String, String>> rows = csvParser.parse(file);
            int created = 0;
            int failed = 0;

            for (Map<String, String> row : rows) {
                try {
                    Map<String, Object> properties = new HashMap<>();
                    for (Map.Entry<String, String> entry : mappings.entrySet()) {
                        String csvColumn = entry.getKey();
                        String propName = entry.getValue();
                        String value = row.get(csvColumn);
                        if (value != null) {
                            properties.put(propName, value);
                        }
                    }

                    String id = UUID.randomUUID().toString();
                    if (!mappings.isEmpty()) {
                        String firstCol = mappings.keySet().iterator().next();
                        String firstVal = row.get(firstCol);
                        if (firstVal != null && !firstVal.isBlank()) {
                            id = "ingest_" + firstVal.replaceAll("\\s+", "_");
                        }
                    }

                    KnowledgeObject obj = new KnowledgeObject(
                        id,
                        row.getOrDefault("name", "Unknown Object"),
                        objectType,
                        row.getOrDefault("description", ""),
                        "system",
                        Instant.now(),
                        1.0,
                        KnowledgeObject.ObjectStatus.ACTIVE,
                        properties
                    );

                    knowledgeService.createObject(obj);
                    created++;

                    // Trigger Active Watcher
                    eventPublisher.publishEvent("OBJECT_CREATED", id, properties);
                } catch (Exception e) {
                    logger.severe("Failed to process row: " + e.getMessage());
                    failed++;
                }
            }

            Map<String, Object> summary = new HashMap<>();
            summary.put("totalProcessed", rows.size());
            summary.put("created", created);
            summary.put("updated", 0);
            summary.put("failed", failed);
            summary.put("status", "COMPLETED");

            return summary;
        } catch (Exception e) {
            logger.severe("Ingestion failed: " + e.getMessage());
            throw new RuntimeException("Ingestion pipeline failed", e);
        }
    }
}
