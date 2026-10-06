package io.orion.ontology.application;

import io.orion.shared.audit.AuditPublisher;
import io.orion.shared.audit.AuditEvent;
import io.orion.shared.tenant.CurrentUser;
import io.orion.ontology.application.port.OntologyLedger;
import io.orion.ontology.domain.OntologyChange;
import io.orion.ontology.domain.OntologyChange.EntityType;
import io.orion.ontology.domain.OntologyChange.ChangeType;
import io.orion.shared.tenant.TenantContext;
import org.springframework.stereotype.Component;
import java.time.Clock;
import java.util.UUID;

@Component
class OntologyChangeRecorder {

    private final OntologyLedger ledger;
    private final AuditPublisher audit;
    private final Clock clock;

    OntologyChangeRecorder(OntologyLedger ledger, AuditPublisher audit, Clock clock) {
        this.ledger = ledger; this.audit = audit; this.clock = clock;
    }

    /** MUST be the first call of every mutation: it takes the per-tenant lock, so what we read afterwards is stable. */
    long begin(UUID tenantId) { return ledger.nextVersion(tenantId); }

    /** Writes the change-log row and the audit event, both in the caller's transaction. */
    void record(long version, EntityType type, UUID entityId, String apiName,
                ChangeType change, Object before, Object after) {
        CurrentUser actor = TenantContext.requireUser();
        ledger.recordChange(new OntologyChange(UUID.randomUUID(), actor.tenantId(), version, type, entityId,
                change, before, after, actor.userId(), clock.instant()));

        audit.publish(AuditEvent.of(type.name() + "_" + change.name()).actorFromContext()
                .resource(resourceType(type), entityId)
                .detail("apiName", apiName).detail("ontologyVersion", version).build());
    }

    private static String resourceType(EntityType t) {
        return switch (t) { case OBJECT_TYPE -> "ObjectType"; case PROPERTY -> "Property"; case LINK_TYPE -> "LinkType"; };
    }
}
