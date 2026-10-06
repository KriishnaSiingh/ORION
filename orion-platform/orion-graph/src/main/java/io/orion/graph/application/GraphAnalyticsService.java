package io.orion.graph.application;

import io.orion.graph.infrastructure.persistence.GDSRepository;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class GraphAnalyticsService {
    private final GDSRepository gdsRepository;

    public GraphAnalyticsService(GDSRepository gdsRepository) {
        this.gdsRepository = gdsRepository;
    }

    public Map<String, Double> computeInfluenceScores() {
        String projection = "influenceProjection";
        try {
            gdsRepository.dropProjection(projection);
            gdsRepository.createProjection(projection, "KnowledgeObject", "LINKED_TO");
            return gdsRepository.runPageRank(projection);
        } finally {
            gdsRepository.dropProjection(projection);
        }
    }

    public Map<String, Long> detectCommunities() {
        String projection = "communityProjection";
        try {
            gdsRepository.dropProjection(projection);
            gdsRepository.createProjection(projection, "KnowledgeObject", "LINKED_TO");
            return gdsRepository.runLouvain(projection);
        } finally {
            gdsRepository.dropProjection(projection);
        }
    }
}
