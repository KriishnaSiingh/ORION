# ADR 0001: Modular Monolith Architecture

## Decision

We have decided to implement the Orion Intelligence Platform as a modular monolith.

## Reason

A modular monolith allows for rapid initial development and simplified deployment while maintaining strict boundaries between domain modules. This approach provides a clear path to migrate specific modules into independent microservices in the future without needing to untangle a highly coupled codebase.

## Rules

1. **Module Boundaries**: Each domain (Identity, Audit, Ontology, Graph) must reside in its own Maven module.
2. **Dependency Direction**: Dependencies must point inward toward the `orion-shared-kernel`. Domain modules must not depend on each other directly.
3. **Bootable Entry**: Only the `orion-app` module is bootable and serves as the aggregation layer.
4. **Hexagonal Layout**: Each module should follow a hexagonal architecture (api, application, domain, infrastructure).
