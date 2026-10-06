package io.orion.ontology.application;

import io.orion.ontology.application.port.LinkTypeRepository;
import io.orion.ontology.application.port.ObjectTypeRepository;
import io.orion.ontology.application.port.OntologyLedger;
import io.orion.ontology.domain.*;
import io.orion.ontology.domain.OntologyChange;
import io.orion.shared.paging.PageResult;
import io.orion.shared.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import java.util.*;

@Service
public class OntologyQueryService {

    public record OntologySnapshot(long version, List<ObjectType> objectTypes, List<LinkType> linkTypes) {}

    private static final int MAX_PAGE_SIZE = 100;

    private final ObjectTypeRepository objectTypes;
    private final LinkTypeRepository linkTypes;
    private final OntologyLedger ledger;

    public OntologyQueryService(ObjectTypeRepository objectTypes, LinkTypeRepository linkTypes, OntologyLedger ledger) {
        this.objectTypes = objectTypes; this.linkTypes = linkTypes; this.ledger = ledger;
    }

    /** REPEATABLE_READ: version and content come from ONE database snapshot, even if an edit commits mid-request. */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public OntologySnapshot snapshot() {
        UUID tenantId = TenantContext.requireTenantId();
        return new OntologySnapshot(ledger.currentVersion(tenantId),
                objectTypes.findAll(tenantId, false), linkTypes.findAll(tenantId, false));
    }

    @Transactional(readOnly = true)
    public PageResult<OntologyChange> changes(int page, int size) {
        return ledger.findChanges(TenantContext.requireTenantId(),
                Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
    }
}
