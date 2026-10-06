package io.orion.shared.error;

public enum ErrorCode {
    VALIDATION_FAILED(400), UNAUTHENTICATED(401), FORBIDDEN(403), NOT_FOUND(404), CONFLICT(409);

    private final int httpStatus;
    ErrorCode(int httpStatus) { this.httpStatus = httpStatus; }
    public int httpStatus() { return httpStatus; }
}
