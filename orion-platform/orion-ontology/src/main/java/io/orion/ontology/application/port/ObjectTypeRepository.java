package io.orion.ontology.application.port;

import io.orion.ontology.domain.ObjectType;
import io.orion.ontology.domain.PropertyDefinition;
import java.util.*;

public interface ObjectTypeRepository {
    void insert(ObjectType type);                                      // 409 on duplicate apiName
    boolean update(ObjectType type, long expectedRevision);            // false if the revision moved
    Optional<ObjectType> findById(UUID tenantId, UUID id);             // includes archived properties
    List<ObjectType> findAll(UUID tenantId, boolean includeArchived);  // types + properties in 2 queries

    void insertProperty(UUID tenantId, UUID objectTypeId, PropertyDefinition property);
    boolean updateProperty(UUID tenantId, UUID objectTypeId, PropertyDefinition property, long expectedRevision);
}
