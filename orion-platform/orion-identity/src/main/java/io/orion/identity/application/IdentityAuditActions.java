package io.orion.identity.application;

public final class IdentityAuditActions {
    private IdentityAuditActions() {}
    public static final String TENANT_REGISTERED      = "TENANT_REGISTERED";
    public static final String LOGIN_SUCCEEDED        = "LOGIN_SUCCEEDED";
    public static final String LOGIN_FAILED           = "LOGIN_FAILED";
    public static final String TOKEN_REUSE_DETECTED   = "TOKEN_REUSE_DETECTED";
    public static final String LOGOUT                 = "LOGOUT";
    public static final String USER_CREATED           = "USER_CREATED";
}
