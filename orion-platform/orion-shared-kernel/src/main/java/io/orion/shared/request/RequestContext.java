package io.orion.shared.request;

public final class RequestContext {
    private static final ThreadLocal<RequestMetadata> HOLDER = new ThreadLocal<>();
    private RequestContext() {}

    public static void set(RequestMetadata m) { HOLDER.set(m); }
    public static RequestMetadata current() {
        RequestMetadata m = HOLDER.get();
        return m == null ? RequestMetadata.NONE : m;
    }
    public static void clear() { HOLDER.remove(); }
}
