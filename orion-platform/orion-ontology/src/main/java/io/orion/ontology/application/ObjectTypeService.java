package io.orion.ontology.application;

import io.orion.shared.tenant.CurrentUser;
import io.orion.ontology.application.port.LinkTypeRepository;
import io.orion.ontology.application.port.ObjectTypeRepository;
import io.orion.ontology.domain.*;
import io.orion.ontology.domain.OntologyChange.EntityType;
import io.orion.ontology.domain.OntologyChange.ChangeType;
import io.orion.shared.tenant.TenantContext;
import io.orion.shared.error.OrionException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.*;

@Service
public class ObjectTypeService {

    public record CreateObjectTypeCommand(String apiName, String displayName, String description, String icon, String color) {}
    public record UpdateObjectTypeCommand(String displayName, String description, String icon, String color, long expectedRevision) {}
    public record CreatePropertyCommand(String apiName, String displayName, String description, DataType dataType,
                                        boolean required, boolean unique, boolean searchable, boolean filterable,
                                        boolean multiValued, List<String> enumValues) {}
    public record UpdatePropertyCommand(String displayName, String description, boolean required, boolean unique,
                                        boolean searchable, boolean filterable, List<String> enumValues, long expectedRevision) {}

    private final ObjectTypeRepository objectTypes;
    private final LinkTypeRepository linkTypes;
    private final OntologyChangeRecorder changes;
    private final Clock clock;

    public ObjectTypeService(ObjectTypeRepository objectTypes, LinkTypeRepository linkTypes,
                             OntologyChangeRecorder changes, Clock clock) {
        this.objectTypes = objectTypes; this.linkTypes = linkTypes; this.changes = changes; this.clock = clock;
    }

    @Transactional
    public ObjectType create(CreateObjectTypeCommand cmd) {
        CurrentUser me = TenantContext.requireUser();
        long version = changes.begin(me.tenantId());
        ObjectType created = ObjectType.create(me.tenantId(), cmd.apiName(), cmd.displayName(), cmd.description(),
                cmd.icon(), cmd.color(), me.userId(), clock.instant());
        objectTypes.insert(created);
        changes.record(version, EntityType.OBJECT_TYPE, created.id(), created.apiName(), ChangeType.CREATED, null, created);
        return created;
    }

    @Transactional
    public ObjectType update(UUID id, UpdateObjectTypeCommand cmd) {
        CurrentUser me = TenantContext.requireUser();
        long version = changes.begin(me.tenantId());
        ObjectType current = load(me.tenantId(), id);
        OntologyGuards.requireActive(current.status(), "Object type");
        OntologyGuards.requireRevision(current.revision(), cmd.expectedRevision(), "Object type");

        ObjectType updated = current.update(cmd.displayName(), cmd.description(), cmd.icon(), cmd.color(),
                me.userId(), clock.instant());
        if (!objectTypes.update(updated, current.revision())) {
            throw OrionException.conflict("Object type was modified concurrently");
        }
        changes.record(version, EntityType.OBJECT_TYPE, id, updated.apiName(), ChangeType.UPDATED,
                current.withProperties(List.of()), updated.withProperties(List.of()));
        return updated.withActivePropertiesOnly();
    }

    @Transactional
    public void archive(UUID id) {
        CurrentUser me = TenantContext.requireUser();
        long version = changes.begin(me.tenantId());
        ObjectType current = load(me.tenantId(), id);
        OntologyGuards.requireActive(current.status(), "Object type");

        List<String> blockers = linkTypes.findActiveReferencing(me.tenantId(), id).stream().map(LinkType::apiName).toList();
        if (!blockers.isEmpty()) {
            throw OrionException.conflict("Cannot archive: referenced by active link types " + blockers);
        }
        ObjectType archived = current.archive(me.userId(), clock.instant());
        if (!objectTypes.update(archived, current.revision())) {
            throw OrionException.conflict("Object type was modified concurrently");
        }
        changes.record(version, EntityType.OBJECT_TYPE, id, current.apiName(), ChangeType.ARCHIVED,
                current.withProperties(List.of()), archived.withProperties(List.of()));
    }

    @Transactional(readOnly = true)
    public ObjectType get(UUID id, boolean includeArchived) {
        ObjectType t = load(TenantContext.requireTenantId(), id);
        return includeArchived ? t : t.withActivePropertiesOnly();
    }

    @Transactional(readOnly = true)
    public List<ObjectType> list(boolean includeArchived) {
        return objectTypes.findAll(TenantContext.requireTenantId(), includeArchived);
    }

    @Transactional
    public PropertyDefinition addProperty(UUID objectTypeId, CreatePropertyCommand cmd) {
        CurrentUser me = TenantContext.requireUser();
        long version = changes.begin(me.tenantId());
        ObjectType type = load(me.tenantId(), objectTypeId);
        OntologyGuards.requireActive(type.status(), "Object type");

        PropertyDefinition p = PropertyDefinition.create(cmd.apiName(), cmd.displayName(), cmd.description(),
                cmd.dataType(), cmd.required(), cmd.unique(), cmd.searchable(), cmd.filterable(),
                cmd.multiValued(), cmd.enumValues(), clock.instant());
        objectTypes.insertProperty(me.tenantId(), objectTypeId, p);
        changes.record(version, EntityType.PROPERTY, p.id(), type.apiName() + "." + p.apiName(),
                ChangeType.CREATED, null, snapshot(objectTypeId, p));
        return p;
    }

    @Transactional
    public PropertyDefinition updateProperty(UUID objectTypeId, UUID propertyId, UpdatePropertyCommand cmd) {
        CurrentUser me = TenantContext.requireUser();
        long version = changes.begin(me.tenantId());
        ObjectType type = load(me.tenantId(), objectTypeId);
        OntologyGuards.requireActive(type.status(), "Object type");
        PropertyDefinition current = type.findProperty(propertyId)
                .orElseThrow(() -> OrionException.notFound("Property not found"));
        OntologyGuards.requireActive(current.status(), "Property");
        OntologyGuards.requireRevision(current.revision(), cmd.expectedRevision(), "Property");

        PropertyDefinition updated = current.update(cmd.displayName(), cmd.description(), cmd.required(), cmd.unique(),
                cmd.searchable(), cmd.filterable(), cmd.enumValues(), clock.instant());
        if (!objectTypes.updateProperty(me.tenantId(), objectTypeId, updated, current.revision())) {
            throw OrionException.conflict("Property was modified concurrently");
        }
        changes.record(version, EntityType.PROPERTY, propertyId, type.apiName() + "." + current.apiName(),
                ChangeType.UPDATED, snapshot(objectTypeId, current), snapshot(objectTypeId, updated));
        return updated;
    }

    @Transactional
    public void archiveProperty(UUID objectTypeId, UUID propertyId) {
        CurrentUser me = TenantContext.requireUser();
        long version = changes.begin(me.tenantId());
        ObjectType type = load(me.tenantId(), objectTypeId);
        PropertyDefinition current = type.findProperty(propertyId)
                .orElseThrow(() -> OrionException.notFound("Property not found"));
        OntologyGuards.requireActive(current.status(), "Property");

        PropertyDefinition archived = current.archive(clock.instant());
        if (!objectTypes.updateProperty(me.tenantId(), objectTypeId, archived, current.revision())) {
            throw OrionException.conflict("Property was modified concurrently");
        }
        changes.record(version, EntityType.PROPERTY, propertyId, type.apiName() + "." + current.apiName(),
                ChangeType.ARCHIVED, snapshot(objectTypeId, current), snapshot(objectTypeId, archived));
    }

    private ObjectType load(UUID tenantId, UUID id) {
        return objectTypes.findById(tenantId, id).orElseThrow(() -> OrionException.notFound("Object type not found"));
    }

    private static Map<String, Object> snapshot(UUID objectTypeId, PropertyDefinition p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("objectTypeId", objectTypeId);
        m.put("property", p);
        return m;
    }
}
