package io.orion.ontology;

public final class OntologyAccess {
    private OntologyAccess() {}
    public static final String CAN_MODIFY = "hasAnyRole('TENANT_ADMIN','ONTOLOGY_DESIGNER')";
}
