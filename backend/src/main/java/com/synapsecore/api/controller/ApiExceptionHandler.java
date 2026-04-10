package com.synapsecore.api.controller;

import com.synapsecore.audit.AuditLogService;
import com.synapsecore.audit.RequestTraceContext;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
@lombok.RequiredArgsConstructor
public class ApiExceptionHandler {

    private final AuditLogService auditLogService;
    private final RequestTraceContext requestTraceContext;

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatus(ResponseStatusException exception,
                                                                HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        auditFailure(request, status, exception.getReason());
        return ResponseEntity.status(status).body(new ApiErrorResponse(
            Instant.now(),
            status.value(),
            status.getReasonPhrase(),
            exception.getReason(),
            requestTraceContext.getRequiredRequestId()
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception,
                                                            HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(error -> error.getField() + " " + error.getDefaultMessage())
            .orElse("Validation failed.");
        auditFailure(request, HttpStatus.BAD_REQUEST, message);
        return ResponseEntity.badRequest().body(new ApiErrorResponse(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            message,
            requestTraceContext.getRequiredRequestId()
        ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException exception,
                                                                        HttpServletRequest request) {
        HttpStatus status = HttpStatus.CONFLICT;
        String message = "The request conflicts with the current SynapseCore operational data.";
        auditFailure(request, status, message);
        return ResponseEntity.status(status).body(new ApiErrorResponse(
            Instant.now(),
            status.value(),
            status.getReasonPhrase(),
            message,
            requestTraceContext.getRequiredRequestId()
        ));
    }

    private void auditFailure(HttpServletRequest request, HttpStatus status, String message) {
        auditLogService.recordFailure(
            "REQUEST_REJECTED",
            "api-client",
            request.getMethod() + " " + request.getRequestURI(),
            "ApiRequest",
            request.getRequestURI(),
            status.value() + " " + message
        );
    }
}
