package io.orion.identity.domain;

public enum SystemRole {
    TENANT_ADMIN("Full administrative control of the tenant"),
    ONTOLOGY_DESIGNER("Defines object types, properties and link types"),
    DATA_ENGINEER("Builds and operates ingestion pipelines"),
    ANALYST("Searches, explores and analyses data"),
    INVESTIGATOR("Works on investigation boards and cases"),
    APP_BUILDER("Builds operational applications"),
    VIEWER("Read-only access");

    private final String description;
    SystemRole(String description) { this.description = description; }
    public String description() { return description; }
}
