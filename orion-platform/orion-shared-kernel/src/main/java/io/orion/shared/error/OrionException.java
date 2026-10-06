package io.orion.shared.error;

public class OrionException extends RuntimeException {
    private final ErrorCode code;

    public OrionException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }
    public ErrorCode code() { return code; }

    public static OrionException validation(String m)      { return new OrionException(ErrorCode.VALIDATION_FAILED, m); }
    public static OrionException unauthenticated(String m) { return new OrionException(ErrorCode.UNAUTHENTICATED, m); }
    public static OrionException forbidden(String m)       { return new OrionException(ErrorCode.FORBIDDEN, m); }
    public static OrionException notFound(String m)        { return new OrionException(ErrorCode.NOT_FOUND, m); }
    public static OrionException conflict(String m)        { return new OrionException(ErrorCode.CONFLICT, m); }
}
