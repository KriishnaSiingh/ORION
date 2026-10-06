package io.orion.app.web;

import io.orion.shared.audit.AuditEvent;
import io.orion.shared.audit.AuditPublisher;
import io.orion.shared.error.ErrorCode;
import io.orion.shared.error.OrionException;
import io.orion.shared.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
class GlobalExceptionHandler {

    private final AuditPublisher audit;

    public GlobalExceptionHandler(AuditPublisher audit) {
        this.audit = audit;
    }

    @ExceptionHandler(OrionException.class)
    ProblemDetail handleOrion(OrionException ex) {
        logException(ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(ex.code().httpStatus()), ex.getMessage());
        pd.setTitle(ex.code().name());
        pd.setProperty("code", ex.code().name());
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        logException(ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
        pd.setTitle(ErrorCode.VALIDATION_FAILED.name());
        pd.setProperty("code", ErrorCode.VALIDATION_FAILED.name());
        pd.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
                .map(e -> Map.of("field", e.getField(), "message", String.valueOf(e.getDefaultMessage())))
                .toList());
        return pd;
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleDenied(AccessDeniedException ex) {
        logException(ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "You do not have permission to perform this action");
        pd.setTitle(ErrorCode.FORBIDDEN.name());
        pd.setProperty("code", ErrorCode.FORBIDDEN.name());
        return pd;
    }

    private void logException(Exception ex) {
        try {
            io.orion.shared.tenant.CurrentUser user = TenantContext.requireUser();
            audit.publish(AuditEvent.of("SYSTEM_ERROR")
                    .tenant(user.tenantId()).actor(user.userId(), user.email())
                    .outcome(AuditEvent.Outcome.FAILURE)
                    .resource("System", "GlobalExceptionHandler")
                    .detail("exception", ex.getClass().getSimpleName())
                    .detail("message", ex.getMessage())
                    .build());
        } catch (Exception ignored) {
            // Fail-safe: don't let auditing of an error cause another error
        }
    }
}
