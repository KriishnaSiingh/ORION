package io.orion.knowledge.application.port;

import io.orion.knowledge.domain.KnowledgeObject;
import io.orion.shared.paging.PageResult;
import java.util.*;
import java.time.Instant;

public interface KnowledgeRepository {
    PageResult<KnowledgeObject> search(String query, String type, int page, int size);
    Optional<KnowledgeObject> findById(String id);
    KnowledgeObject save(KnowledgeObject object);
    List<KnowledgeObject> findLinked(String id, int depth);
    void createLink(String sourceId, String targetId, String linkType);
    void createLink(String sourceId, String targetId, String linkType, Instant createdAt);
    List<KnowledgeObject> findLinkedAt(String id, int depth, Instant atTime);
}
