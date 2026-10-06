package io.orion.ontology.application;

import io.orion.shared.error.OrionException;

final class OntologyGuards {
    private OntologyGuards() {}

    static void requireRevision(long actual, long expected, String what) {
        if (actual != expected) {
            throw OrionException.conflict(what + " was modified by someone else (current revision " + actual + "). Reload and retry.");
        }
    }
    static void requireActive(io.orion.ontology.domain.EntityStatus status, String what) {
        if (status != io.orion.ontology.domain.EntityStatus.ACTIVE) throw OrionException.conflict(what + " is archived");
    }
}
