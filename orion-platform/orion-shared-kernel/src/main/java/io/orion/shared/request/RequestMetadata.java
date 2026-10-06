package io.orion.shared.request;

public record RequestMetadata(String requestId, String ipAddress, String userAgent) {
    public static final RequestMetadata NONE = new RequestMetadata(null, null, null);
}
