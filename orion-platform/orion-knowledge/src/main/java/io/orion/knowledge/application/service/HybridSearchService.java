package io.orion.knowledge.application.service;

import io.orion.knowledge.application.port.KnowledgeRepository;
import io.orion.knowledge.domain.KnowledgeObject;
import io.orion.knowledge.infrastructure.vector.VectorService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class HybridSearchService {
    private final KnowledgeRepository repository;
    private final VectorService vectorService;

    public HybridSearchService(KnowledgeRepository repository, VectorService vectorService) {
        this.repository = repository;
        this.vectorService = vectorService;
    }

    public List<KnowledgeObject> hybridSearch(String query, String type, int limit) {
        // 1. Keyword Search (using existing repository.search)
        // We fetch a larger set for RRF reranking
        var keywordResults = repository.search(query, type, 0, limit * 2).items();

        // 2. Vector Search (Simulated for Demo)
        // In a real system, this would be a Cypher query using db.index.vector.queryNodes
        float[] queryVector = vectorService.generateEmbedding(query);
        List<KnowledgeObject> vectorResults = simulateVectorSearch(queryVector, type, limit * 2);

        // 3. RRF (Reciprocal Rank Fusion)
        return mergeResults(keywordResults, vectorResults, limit);
    }

    private List<KnowledgeObject> simulateVectorSearch(float[] vector, String type, int limit) {
        // For demo, we just take a slice of existing objects and shuffle them
        // as if they were semantically related.
        var all = repository.search(null, type, 0, 100).items();
        List<KnowledgeObject> shuffled = new ArrayList<>(all);
        Collections.shuffle(shuffled);
        return shuffled.stream().limit(limit).collect(Collectors.toList());
    }

    private List<KnowledgeObject> mergeResults(List<KnowledgeObject> keyword, List<KnowledgeObject> vector, int limit) {
        Map<String, Double> scores = new HashMap<>();
        double k = 60.0; // RRF constant

        for (int i = 0; i < keyword.size(); i++) {
            String id = keyword.get(i).id();
            scores.put(id, scores.getOrDefault(id, 0.0) + 1.0 / (k + i + 1));
        }

        for (int i = 0; i < vector.size(); i++) {
            String id = vector.get(i).id();
            scores.put(id, scores.getOrDefault(id, 0.0) + 1.0 / (k + i + 1));
        }

        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> {
                    try {
                        return repository.findById(entry.getKey()).orElse(null);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
