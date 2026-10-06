package io.orion.ingestion.infrastructure.event;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;
import java.util.logging.Logger;

@Service
public class IngestionEventPublisher {
    private static final Logger logger = Logger.getLogger(IngestionEventPublisher.class.getName());
    private final RestTemplate restTemplate = new RestTemplate();
    private final String AI_ORCHESTRATOR_URL = "http://localhost:8001/investigate";

    public void publishEvent(String eventType, String objectId, Map<String, Object> properties) {
        logger.info("Publishing event: " + eventType + " for object: " + objectId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", eventType);
        payload.put("objectId", objectId);
        payload.put("properties", properties);
        payload.put("timestamp", System.currentTimeMillis());

        try {
            // Async call to AI Orchestrator to trigger an investigation
            restTemplate.postForEntity(AI_ORCHESTRATOR_URL, payload, Void.class);
        } catch (Exception e) {
            logger.warning("Failed to notify AI Orchestrator: " + e.getMessage());
        }
    }
}
