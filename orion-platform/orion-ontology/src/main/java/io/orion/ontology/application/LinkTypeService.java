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
import java.util.*;

@Service
public class LinkTypeService {

    public record CreateLinkTypeCommand(String apiName, String displayName, String description,
                                        UUID sourceObjectTypeId, UUID targetObjectTypeId,
                                        Cardinality cardinality, boolean bidirectional) {}
    public record UpdateLinkTypeCommand(String displayName, String description, Cardinality cardinality,
                                        boolean bidirectional, long expectedRevision) {}

    private final LinkTypeRepository linkTypes;
    private final ObjectTypeRepository objectTypes;
    private final OntologyChangeRecorder changes;
    private final Clock clock;

    public LinkTypeService(LinkTypeRepository linkTypes, ObjectTypeRepository objectTypes,
                           OntologyChangeRecorder changes, Clock clock) {
        this.linkTypes = linkTypes; this.objectTypes = objectTypes; this.changes = changes; this.clock = clock;
    }

    @Transactional
    public LinkType create(CreateLinkTypeCommand cmd) {
        CurrentUser me = TenantContext.requireUser();
        long version = changes.begin(me.tenantId());
        requireActiveEndpoint(me.tenantId(), cmd.sourceObjectTypeId(), "source");
        requireActiveEndpoint(me.tenantId(), cmd.targetObjectTypeId(), "target");

        LinkType created = LinkType.create(me.tenantId(), cmd.apiName(), cmd.displayName(), cmd.description(),
                cmd.sourceObjectTypeId(), cmd.targetObjectTypeId(), cmd.cardinality(), cmd.bidirectional(),
                me.userId(), clock.instant());
        linkTypes.insert(created);
        changes.record(version, EntityType.LINK_TYPE, created.id(), created.apiName(), ChangeType.CREATED, null, created);
        return created;
    }

    @Transactional
    public LinkType update(UUID id, UpdateLinkTypeCommand cmd) {
        CurrentUser me = TenantContext.requireUser();
        long version = changes.begin(me.tenantId());
        LinkType current = load(me.tenantId(), id);
        OntologyGuards.requireActive(current.status(), "Link type");
        OntologyGuards.requireRevision(current.revision(), cmd.expectedRevision(), "Link type");

        LinkType updated = current.update(cmd.displayName(), cmd.description(), cmd.cardinality(),
                cmd.bidirectional(), me.userId(), clock.instant());
        if (!linkTypes.update(updated, current.revision())) {
            throw OrionException.conflict("Link type was modified concurrently");
        }
        changes.record(version, EntityType.LINK_TYPE, id, updated.apiName(), ChangeType.UPDATED, current, updated);
        return updated;
    }

    @Transactional
    public void archive(UUID id) {
        CurrentUser me = TenantContext.requireUser();
        long version = changes.begin(me.tenantId());
        LinkType current = load(me.tenantId(), id);
        OntologyGuards.requireActive(current.status(), "Link type");

        LinkType archived = current.archive(me.userId(), clock.instant());
        if (!linkTypes.update(archived, current.revision())) {
            throw OrionException.conflict("Link type was modified concurrently");
        }
        changes.record(version, EntityType.LINK_TYPE, id, current.apiName(), ChangeType.ARCHIVED, current, archived);
    }

    @Transactional(readOnly = true)
    public LinkType get(UUID id) { return load(TenantContext.requireTenantId(), id); }

    @Transactional(readOnly = true)
    public List<LinkType> list(boolean includeArchived) {
        return linkTypes.findAll(TenantContext.requireTenantId(), includeArchived);
    }

    private LinkType load(UUID tenantId, UUID id) {
        return linkTypes.findById(tenantId, id).orElseThrow(() -> OrionException.notFound("Link type not found"));
    }

    private void requireActiveEndpoint(UUID tenantId, UUID objectTypeId, String role) {
        objectTypes.findById(tenantId, objectTypeId).filter(ObjectType::isActive)
                .orElseThrow(() -> OrionException.validation("Unknown or archived " + role + " object type"));
    }
}
