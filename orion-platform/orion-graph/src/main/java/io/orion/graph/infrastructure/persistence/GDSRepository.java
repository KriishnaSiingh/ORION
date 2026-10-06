package io.orion.graph.infrastructure.persistence;

import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class GDSRepository {
    private final Neo4jClient client;

    public GDSRepository(Neo4jClient client) {
        this.client = client;
    }

    public void createProjection(String name, String nodeLabel, String relType) {
        String cypher = String.format(
            "CALL gds.graph.project('%s', '%s', '%s')",
            name, nodeLabel, relType
        );
        client.query(cypher).run();
    }

    public void dropProjection(String name) {
        String cypher = String.format("CALL gds.graph.drop('%s', false)", name);
        client.query(cypher).run();
    }

    public Map<String, Double> runPageRank(String projectionName) {
        String cypher = String.format(
            "CALL gds.pageRank.stream('%s') " +
            "YIELD nodeId, score " +
            "RETURN gds.util.asNode(nodeId).id AS id, score ORDER BY score DESC",
            projectionName
        );

        Collection<Map<String, Object>> results = client.query(cypher).fetch().all();
        Map<String, Double> scores = new HashMap<>();
        for (Map<String, Object> row : results) {
            scores.put((String) row.get("id"), (Double) row.get("score"));
        }
        return scores;
    }

    public Map<String, Long> runLouvain(String projectionName) {
        String cypher = String.format(
            "CALL gds.louvain.stream('%s') " +
            "YIELD nodeId, communityId " +
            "RETURN gds.util.asNode(nodeId).id AS id, communityId",
            projectionName
        );

        Collection<Map<String, Object>> results = client.query(cypher).fetch().all();
        Map<String, Long> communities = new HashMap<>();
        for (Map<String, Object> row : results) {
            communities.put((String) row.get("id"), (Long) row.get("communityId"));
        }
        return communities;
    }
}
