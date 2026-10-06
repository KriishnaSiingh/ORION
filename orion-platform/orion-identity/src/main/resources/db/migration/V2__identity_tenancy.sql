CREATE TABLE tenants (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(200) NOT NULL,
    slug        VARCHAR(63)  NOT NULL UNIQUE,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_tenants_slug CHECK (slug ~ '^[a-z0-9]([a-z0-9-]{1,61}[a-z0-9])$')
);

CREATE TABLE roles (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID         NOT NULL REFERENCES tenants(id),
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    is_system   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_roles_tenant_name UNIQUE (tenant_id, name),
    CONSTRAINT uq_roles_id_tenant   UNIQUE (id, tenant_id)
);

CREATE TABLE users (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id      UUID         NOT NULL REFERENCES tenants(id),
    email          VARCHAR(320) NOT NULL,
    name           VARCHAR(200) NOT NULL,
    password_hash  VARCHAR(100) NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_login_at  TIMESTAMPTZ,
    CONSTRAINT uq_users_tenant_email UNIQUE (tenant_id, email),
    CONSTRAINT uq_users_id_tenant    UNIQUE (id, tenant_id),
    CONSTRAINT ck_users_email_lower  CHECK (email = lower(email))
);

CREATE TABLE user_roles (
    user_id    UUID NOT NULL,
    role_id    UUID NOT NULL,
    tenant_id  UUID NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id, tenant_id) REFERENCES users (id, tenant_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id, tenant_id) REFERENCES roles (id, tenant_id) ON DELETE CASCADE
);

CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY,
    tenant_id   UUID        NOT NULL,
    user_id     UUID        NOT NULL,
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id, tenant_id) REFERENCES users (id, tenant_id) ON DELETE CASCADE
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (tenant_id, user_id) WHERE revoked_at IS NULL;
