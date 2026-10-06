-- One row per tenant; bumped (and row-locked) by every ontology mutation.
CREATE TABLE ontology_versions (
    tenant_id       UUID PRIMARY KEY REFERENCES tenants(id),
    current_version BIGINT      NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE object_types (
    id           UUID PRIMARY KEY,
    tenant_id    UUID         NOT NULL REFERENCES tenants(id),
    api_name     VARCHAR(63)  NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    description  VARCHAR(1000),
    icon         VARCHAR(50),
    color        VARCHAR(7),
    status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    revision     BIGINT       NOT NULL DEFAULT 1,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,
    created_by   UUID         NOT NULL,     -- no FK to users: ontology stays decoupled from identity
    updated_by   UUID         NOT NULL,
    CONSTRAINT uq_object_types_tenant_api UNIQUE (tenant_id, api_name),
    CONSTRAINT uq_object_types_id_tenant  UNIQUE (id, tenant_id),
    CONSTRAINT ck_object_types_api_name   CHECK (api_name ~ '^[A-Z][A-Za-z0-9]{1,62}$'),
    CONSTRAINT ck_object_types_status     CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    CONSTRAINT ck_object_types_color      CHECK (color IS NULL OR color ~ '^#[0-9A-Fa-f]{6}$')
);

CREATE TABLE properties (
    id              UUID PRIMARY KEY,
    tenant_id       UUID         NOT NULL,
    object_type_id  UUID         NOT NULL,
    api_name        VARCHAR(63)  NOT NULL,
    display_name    VARCHAR(100) NOT NULL,
    description     VARCHAR(1000),
    data_type       VARCHAR(20)  NOT NULL,
    is_required     BOOLEAN      NOT NULL DEFAULT FALSE,
    is_unique       BOOLEAN      NOT NULL DEFAULT FALSE,
    is_searchable   BOOLEAN      NOT NULL DEFAULT FALSE,
    is_filterable   BOOLEAN      NOT NULL DEFAULT FALSE,
    is_multi_valued BOOLEAN      NOT NULL DEFAULT FALSE,
    config          JSONB        NOT NULL DEFAULT '{}'::jsonb,   -- {"enumValues": [...]}; room for validation rules later
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    revision        BIGINT       NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_properties_object_type FOREIGN KEY (object_type_id, tenant_id)
        REFERENCES object_types (id, tenant_id),
    CONSTRAINT uq_properties_type_api    UNIQUE (object_type_id, api_name),
    CONSTRAINT ck_properties_api_name    CHECK (api_name ~ '^[a-z][A-Za-z0-9]{1,62}$'),
    CONSTRAINT ck_properties_data_type   CHECK (data_type IN
        ('STRING','INTEGER','DECIMAL','BOOLEAN','DATE','DATETIME','GEOPOINT','ENUM')),
    CONSTRAINT ck_properties_status      CHECK (status IN ('ACTIVE', 'ARCHIVED'))
);
CREATE INDEX idx_properties_tenant_type ON properties (tenant_id, object_type_id);

CREATE TABLE link_types (
    id                    UUID PRIMARY KEY,
    tenant_id             UUID         NOT NULL REFERENCES tenants(id),
    api_name              VARCHAR(63)  NOT NULL,
    display_name          VARCHAR(100) NOT NULL,
    description           VARCHAR(1000),
    source_object_type_id UUID         NOT NULL,
    target_object_type_id UUID         NOT NULL,
    cardinality           VARCHAR(20)  NOT NULL,
    is_bidirectional      BOOLEAN      NOT NULL DEFAULT FALSE,
    status                VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    revision              BIGINT       NOT NULL DEFAULT 1,
    created_at            TIMESTAMPTZ  NOT NULL,
    updated_at            TIMESTAMPTZ  NOT NULL,
    created_by            UUID         NOT NULL,
    updated_by            UUID         NOT NULL,
    -- Composite FKs: both endpoints must belong to the SAME tenant as the link type.
    CONSTRAINT fk_link_types_source FOREIGN KEY (source_object_type_id, tenant_id) REFERENCES object_types (id, tenant_id),
    CONSTRAINT fk_link_types_target FOREIGN KEY (target_object_type_id, tenant_id) REFERENCES object_types (id, tenant_id),
    CONSTRAINT uq_link_types_tenant_api UNIQUE (tenant_id, api_name),
    CONSTRAINT uq_link_types_id_tenant  UNIQUE (id, tenant_id),
    CONSTRAINT ck_link_types_api_name   CHECK (api_name ~ '^[A-Z][A-Z0-9_]{1,62}$'),
    CONSTRAINT ck_link_types_cardinality CHECK (cardinality IN ('ONE_TO_ONE','ONE_TO_MANY','MANY_TO_MANY')),
    CONSTRAINT ck_link_types_status     CHECK (status IN ('ACTIVE', 'ARCHIVED'))
);
CREATE INDEX idx_link_types_source ON link_types (tenant_id, source_object_type_id);
CREATE INDEX idx_link_types_target ON link_types (tenant_id, target_object_type_id);

-- Domain history: one row per ontology version (each mutation = exactly one version).
CREATE TABLE ontology_changes (
    id               UUID PRIMARY KEY,
    tenant_id        UUID         NOT NULL REFERENCES tenants(id),
    ontology_version BIGINT       NOT NULL,
    entity_type      VARCHAR(20)  NOT NULL,
    entity_id        UUID         NOT NULL,
    change_type      VARCHAR(20)  NOT NULL,
    snapshot_before  JSONB,
    snapshot_after   JSONB,
    changed_by       UUID         NOT NULL,
    changed_at       TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_ontology_changes_version UNIQUE (tenant_id, ontology_version),
    CONSTRAINT ck_ontology_changes_entity  CHECK (entity_type IN ('OBJECT_TYPE','PROPERTY','LINK_TYPE')),
    CONSTRAINT ck_ontology_changes_change  CHECK (change_type IN ('CREATED','UPDATED','ARCHIVED'))
);
CREATE INDEX idx_ontology_changes_entity ON ontology_changes (tenant_id, entity_type, entity_id);

CREATE FUNCTION ontology_changes_reject_mutation() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'ontology_changes is append-only (% not allowed)', TG_OP USING ERRCODE = 'restrict_violation';
END
$$;

CREATE TRIGGER trg_ontology_changes_no_update_delete
    BEFORE UPDATE OR DELETE ON ontology_changes
    FOR EACH ROW EXECUTE FUNCTION ontology_changes_reject_mutation();

CREATE TRIGGER trg_ontology_changes_no_truncate
    BEFORE TRUNCATE ON ontology_changes
    FOR EACH STATEMENT EXECUTE FUNCTION ontology_changes_reject_mutation();
