package io.orion.knowledge.application;

import io.orion.knowledge.application.port.KnowledgeRepository;
import io.orion.knowledge.domain.KnowledgeObject;
import io.orion.ontology.application.OntologyQueryService;
import io.orion.ontology.domain.LinkType;
import io.orion.shared.paging.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class KnowledgeService {
    private final KnowledgeRepository repository;
    private final OntologyQueryService ontologyService;

    public KnowledgeService(KnowledgeRepository repository, OntologyQueryService ontologyService) {
        this.repository = repository;
        this.ontologyService = ontologyService;
    }

    @Transactional(readOnly = true)
    public PageResult<KnowledgeObject> search(String query, String type, int page, int size) {
        return repository.search(query, type, page, size);
    }

    @Transactional(readOnly = true)
    public KnowledgeObject getObject(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Object not found: " + id));
    }

    @Transactional
    public KnowledgeObject createObject(KnowledgeObject object) {
        validateObjectType(object.type());
        return repository.save(object);
    }

    @Transactional
    public void createLink(String sourceId, String targetId, String linkTypeApiName) {
        // 1. Validate Link Type exists
        LinkType linkType = ontologyService.snapshot().linkTypes().stream()
                .filter(lt -> lt.apiName().equals(linkTypeApiName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Invalid Link Type: " + linkTypeApiName));

        // 2. Verify objects exist
        KnowledgeObject source = getObject(sourceId);
        KnowledgeObject target = getObject(targetId);

        // 3. Validate type compatibility
        // Note: LinkType uses UUIDs for source/target object type IDs,
        // but KnowledgeObject uses the API Name (String) as the type.
        // We need to resolve the API Name from the UUID in the ontology.

        String sourceTypeName = ontologyService.snapshot().objectTypes().stream()
                .filter(ot -> ot.id().equals(linkType.sourceObjectTypeId()))
                .map(io.orion.ontology.domain.ObjectType::apiName)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not resolve source object type ID"));

        String targetTypeName = ontologyService.snapshot().objectTypes().stream()
                .filter(ot -> ot.id().equals(linkType.targetObjectTypeId()))
                .map(io.orion.ontology.domain.ObjectType::apiName)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not resolve target object type ID"));

        if (!source.type().equals(sourceTypeName)) {
            throw new RuntimeException(String.format(
                "Source object type [%s] is incompatible with link type [%s] (expected [%s])",
                source.type(), linkTypeApiName, sourceTypeName));
        }
        if (!target.type().equals(targetTypeName)) {
            throw new RuntimeException(String.format(
                "Target object type [%s] is incompatible with link type [%s] (expected [%s])",
                target.type(), linkTypeApiName, targetTypeName));
        }

        repository.createLink(sourceId, targetId, linkTypeApiName);
    }

    @Transactional(readOnly = true)
    public List<KnowledgeObject> getRelated(String id) {
        return repository.findLinked(id, 1);
    }

    private void validateObjectType(String type) {
        boolean exists = ontologyService.snapshot().objectTypes().stream()
                .anyMatch(ot -> ot.apiName().equals(type));
        if (!exists) {
            throw new RuntimeException("Invalid Object Type: " + type + ". Type must be defined in the Ontology.");
        }
    }
}
