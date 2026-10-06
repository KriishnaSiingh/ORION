package io.orion.knowledge.infrastructure.persistence;

import io.orion.knowledge.application.port.KnowledgeRepository;
import io.orion.knowledge.domain.KnowledgeObject;
import io.orion.shared.paging.PageResult;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.time.Instant;

@Repository
public class Neo4jKnowledgeRepository implements KnowledgeRepository {
    private final Neo4jClient client;
    private final Neo4jTemplate template;

    public Neo4jKnowledgeRepository(Neo4jClient client, Neo4jTemplate template) {
        this.client = client;
        this.template = template;
    }

    @Override
    public PageResult<KnowledgeObject> search(String query, String type, int page, int size) {
        String cypher = "MATCH (n:KnowledgeObject) ";
        if (type != null) cypher += "WHERE n.type = $type ";
        if (query != null) cypher += "AND (n.name CONTAINS $query OR n.description CONTAINS $query) ";
        cypher += "RETURN n SKIP $skip LIMIT $limit";

        Map<String, Object> params = new HashMap<>();
        params.put("type", type);
        params.put("query", query);
        params.put("skip", page * size);
        params.put("limit", size);

        List<KnowledgeObject> results = new ArrayList<>(client.query(cypher).bindAll(params).fetchAs(KnowledgeObject.class).all());

        // In a real impl, we would do a separate count query for total
        return new PageResult<>(results, page, size, results.size());
    }

    @Override
    public Optional<KnowledgeObject> findById(String id) {
        return template.findById(id, KnowledgeObject.class);
    }

    @Override
    public KnowledgeObject save(KnowledgeObject object) {
        // Use dynamic label based on the object type + the general KnowledgeObject label
        String label = object.type();
        String cypher = String.format(
            "MERGE (n:KnowledgeObject {id: $id}) " +
            "SET n:%s, n.name = $name, n.type = $type, n.description = $description, n.owner = $owner, n.updatedAt = $updatedAt, n.confidence = $confidence, n.status = $status",
            label
        );

        client.query(cypher)
                .bind(object.id()).to("id")
                .bind(object.name()).to("name")
                .bind(object.type()).to("type")
                .bind(object.description()).to("description")
                .bind(object.owner()).to("owner")
                .bind(object.updatedAt().toString()).to("updatedAt")
                .bind(object.confidence()).to("confidence")
                .bind(object.status().name()).to("status")
                .run();
        return object;
    }

    @Override
    public List<KnowledgeObject> findLinked(String id, int depth) {
        String cypher = "MATCH (n:KnowledgeObject {id: $id})-[r*1.." + depth + "]-(m:KnowledgeObject) RETURN m";
        return new ArrayList<>(client.query(cypher).bind(id).to("id").fetchAs(KnowledgeObject.class).all());
    }

    @Override
    public void createLink(String sourceId, String targetId, String linkType) {
        createLink(sourceId, targetId, linkType, Instant.now());
    }

    @Override
    public void createLink(String sourceId, String targetId, String linkType, Instant createdAt) {
        String cypher = String.format(
            "MATCH (a:KnowledgeObject {id: $sourceId}), (b:KnowledgeObject {id: $targetId}) " +
            "MERGE (a)-[r:%s]->(b) " +
            "SET r.createdAt = $createdAt",
            linkType
        );
        client.query(cypher)
                .bind(sourceId).to("sourceId")
                .bind(targetId).to("targetId")
                .bind(createdAt.toString()).to("createdAt")
                .run();
    }

    @Override
    public List<KnowledgeObject> findLinkedAt(String id, int depth, Instant atTime) {
        String cypher = String.format(
            "MATCH (n:KnowledgeObject {id: $id})-[r*1..%d]-(m:KnowledgeObject) " +
            "WHERE all(rel IN r WHERE rel.createdAt <= $atTime) " +
            "RETURN m",
            depth
        );
        return new ArrayList<>(client.query(cypher)
                .bind(id).to("id")
                .bind(atTime.toString()).to("atTime")
                .fetchAs(KnowledgeObject.class).all());
    }
}
