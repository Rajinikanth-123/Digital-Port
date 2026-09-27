package com.portshipping.config;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    private ErrorResponse body(HttpStatus s, String m, HttpServletRequest r) {
        return new ErrorResponse(Instant.now(), s.value(), s.getReasonPhrase(), m, r.getRequestURI());
    }

    private ErrorResponse body(HttpStatus s, String m, HttpServletRequest r, Map<String, String> details) {
        return new ErrorResponse(Instant.now(), s.value(), s.getReasonPhrase(), m, r.getRequestURI(), details);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> handleResponseStatus(ResponseStatusException e, HttpServletRequest r) {
        HttpStatus status = HttpStatus.valueOf(e.getStatusCode().value());
        String message = e.getReason() != null ? e.getReason() : e.getMessage();
        return ResponseEntity.status(status).body(body(status, message, r));
    }

    @ExceptionHandler(com.portshipping.exception.NotFoundException.class)
    public ResponseEntity<?> missing(Exception e, HttpServletRequest r) {
        return ResponseEntity.status(404).body(body(HttpStatus.NOT_FOUND, e.getMessage(), r));
    }

    @ExceptionHandler(com.portshipping.exception.ConflictException.class)
    public ResponseEntity<?> conflictRequest(Exception e, HttpServletRequest r) {
        return ResponseEntity.status(409).body(body(HttpStatus.CONFLICT, e.getMessage(), r));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> bad(Exception e, HttpServletRequest r) {
        return ResponseEntity.badRequest().body(body(HttpStatus.BAD_REQUEST, e.getMessage(), r));
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<?> unauthorized(Exception e, HttpServletRequest r) {
        return ResponseEntity.status(401).body(body(HttpStatus.UNAUTHORIZED, "Invalid email or password", r));
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<?> conflict(Exception e, HttpServletRequest r) {
        return ResponseEntity.status(409).body(body(HttpStatus.CONFLICT, "The request conflicts with an existing database record or relationship.", r));
    }

    @ExceptionHandler(org.springframework.jdbc.CannotGetJdbcConnectionException.class)
    public ResponseEntity<?> unavailable(Exception e, HttpServletRequest r) {
        return ResponseEntity.status(503).body(body(HttpStatus.SERVICE_UNAVAILABLE, "The database is unavailable.", r));
    }

    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    public ResponseEntity<?> databaseError(Exception e, HttpServletRequest r) {
        return ResponseEntity.status(503).body(body(HttpStatus.SERVICE_UNAVAILABLE, "The database schema or connection is unavailable.", r));
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<?> validation(org.springframework.web.bind.MethodArgumentNotValidException e, HttpServletRequest r) {
        Map<String, String> details = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(x -> details.putIfAbsent(x.getField(), x.getDefaultMessage()));
        return ResponseEntity.badRequest().body(body(HttpStatus.BAD_REQUEST, "Validation failed", r, details));
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> type(Exception e, HttpServletRequest r) {
        return ResponseEntity.badRequest().body(body(HttpStatus.BAD_REQUEST, "Invalid request parameter", r));
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<?> methodNotSupported(Exception e, HttpServletRequest r) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(body(HttpStatus.METHOD_NOT_ALLOWED, e.getMessage(), r));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> unexpected(Exception e, HttpServletRequest r) {
        return ResponseEntity.status(500).body(body(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", r));
    }
}
