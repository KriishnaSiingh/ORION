# ADR 0002: Identity and Tenancy

## Decision

Implement a strong multi-tenant identity system where isolation is enforced at the database and application levels.

## Key Principles

1. **JWT-Based Identity**: Tenant identity is resolved exclusively from the signed JWT.
2. **Per-Tenant Uniqueness**: Email uniqueness is enforced per tenant, allowing the same email to exist in different tenants.
3. **Composite Isolation**: Database tables for roles and tokens use composite foreign keys (id, tenant_id) to prevent cross-tenant assignments.
4. **JDBC for Identity**: Using `JdbcClient` instead of JPA for the identity module to ensure all `tenant_id` predicates are explicit and auditable.
5. **Token Security**: Implementation of refresh token rotation with reuse detection to prevent theft.
6. **Modular Migrations**: Each module manages its own migrations while maintaining a global version sequence.
