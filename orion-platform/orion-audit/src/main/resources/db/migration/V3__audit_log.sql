CREATE TABLE audit_logs (
    id             UUID PRIMARY KEY,
    tenant_id      UUID,                                  -- NULL for events with no resolvable tenant; no FK on purpose
    actor_id       UUID,
    actor_email    VARCHAR(320),
    action         VARCHAR(100) NOT NULL,
    outcome        VARCHAR(20)  NOT NULL,
    resource_type  VARCHAR(100),
    resource_id    VARCHAR(200),
    details        JSONB        NOT NULL DEFAULT '{}'::jsonb,
    ip_address     VARCHAR(45),
    user_agent     VARCHAR(500),
    request_id     VARCHAR(64),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_audit_outcome CHECK (outcome IN ('SUCCESS', 'FAILURE', 'DENIED'))
);

CREATE INDEX idx_audit_tenant_time     ON audit_logs (tenant_id, created_at DESC);
CREATE INDEX idx_audit_tenant_actor    ON audit_logs (tenant_id, actor_id, created_at DESC);
CREATE INDEX idx_audit_tenant_action   ON audit_logs (tenant_id, action, created_at DESC);
CREATE INDEX idx_audit_tenant_resource ON audit_logs (tenant_id, resource_type, resource_id);

-- Append-only: history cannot be rewritten through SQL, even by application bugs.
CREATE FUNCTION audit_logs_reject_mutation() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'audit_logs is append-only (% not allowed)', TG_OP USING ERRCODE = 'restrict_violation';
END
$$;

CREATE TRIGGER trg_audit_no_update_delete
    BEFORE UPDATE OR DELETE ON audit_logs
    FOR EACH ROW EXECUTE FUNCTION audit_logs_reject_mutation();

CREATE TRIGGER trg_audit_no_truncate
    BEFORE TRUNCATE ON audit_logs
    FOR EACH STATEMENT EXECUTE FUNCTION audit_logs_reject_mutation();
