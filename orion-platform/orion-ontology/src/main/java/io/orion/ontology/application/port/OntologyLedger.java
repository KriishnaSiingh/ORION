package io.orion.ontology.application.port;

import io.orion.ontology.domain.OntologyChange;
import io.orion.shared.paging.PageResult;
import java.util.UUID;

public interface OntologyLedger {
    /** Atomically increments the tenant's ontology version. Holds a row lock until the transaction ends. */
    long nextVersion(UUID tenantId);
    long currentVersion(UUID tenantId);                                // 0 if the tenant has no ontology yet
    void recordChange(OntologyChange change);
    PageResult<OntologyChange> findChanges(UUID tenantId, int page, int size);
}
