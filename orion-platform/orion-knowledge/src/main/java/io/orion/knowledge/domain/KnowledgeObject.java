package io.orion.knowledge.domain;

import java.util.*;
import java.time.Instant;

public record KnowledgeObject(
    String id,
    String name,
    String type,
    String description,
    String owner,
    Instant updatedAt,
    double confidence,
    ObjectStatus status,
    Map<String, Object> properties
) {
    public enum ObjectStatus {
        OPERATIONAL, ACTIVE, REVIEW, DRAFT, PAUSED, WARNING;

        public String toFrontend() {
            return switch(this) {
                case OPERATIONAL -> "Operational";
                case ACTIVE -> "Active";
                case REVIEW -> "Review";
                case DRAFT -> "Draft";
                case PAUSED -> "Paused";
                case WARNING -> "Warning";
            };
        }
    }
}
