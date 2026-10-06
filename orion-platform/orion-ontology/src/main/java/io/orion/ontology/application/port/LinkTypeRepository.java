package io.orion.ontology.application.port;

import io.orion.ontology.domain.LinkType;
import java.util.*;

public interface LinkTypeRepository {
    void insert(LinkType linkType);
    boolean update(LinkType linkType, long expectedRevision);
    Optional<LinkType> findById(UUID tenantId, UUID id);
    List<LinkType> findAll(UUID tenantId, boolean includeArchived);
    List<LinkType> findActiveReferencing(UUID tenantId, UUID objectTypeId);
}
