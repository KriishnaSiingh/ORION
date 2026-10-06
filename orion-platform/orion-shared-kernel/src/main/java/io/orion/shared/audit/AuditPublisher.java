package io.orion.shared.audit;

/**
 * Records an audit event. Contract:
 *  - synchronous and joins the caller's transaction (if any)
 *  - fail-closed: throws if the event cannot be persisted
 *  - never pass secrets (passwords, tokens) in event details
 */
public interface AuditPublisher {
    void publish(AuditEvent event);
}
