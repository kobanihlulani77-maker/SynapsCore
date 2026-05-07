package com.synapsecore.api.controller;

import com.synapsecore.audit.RequestTraceContext;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${server.error.path:${error.path:/error}}")
@RequiredArgsConstructor
@Slf4j
public class FrameworkErrorController implements ErrorController {

    private final RequestTraceContext requestTraceContext;

    @RequestMapping
    public ResponseEntity<ApiErrorResponse> handleError(HttpServletRequest request) {
        Throwable exception = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        if (isAbortLike(exception)) {
            log.debug("Suppressing abort-like framework error dispatch for request [{}]: {}",
                resolveRequestId(),
                exception == null ? "no exception detail" : exception.getMessage());
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        HttpStatus status = resolveStatus(request);
        String requestId = resolveRequestId();
        return ResponseEntity.status(status).body(new ApiErrorResponse(
            Instant.now(),
            status.value(),
            status.getReasonPhrase(),
            "The request failed while SynapseCore was handling an error response.",
            requestId
        ));
    }

    private HttpStatus resolveStatus(HttpServletRequest request) {
        Object rawStatus = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (rawStatus instanceof Integer statusCode) {
            try {
                return HttpStatus.valueOf(statusCode);
            } catch (IllegalArgumentException ignored) {
                return HttpStatus.INTERNAL_SERVER_ERROR;
            }
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String resolveRequestId() {
        return requestTraceContext.getCurrentRequestId().orElse("system-no-request");
    }

    private boolean isAbortLike(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase();
                if (normalized.contains("broken pipe")
                    || normalized.contains("connection reset by peer")
                    || normalized.contains("an existing connection was forcibly closed")
                    || normalized.contains("an established connection was aborted")
                    || normalized.contains("session was invalidated")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }
}
