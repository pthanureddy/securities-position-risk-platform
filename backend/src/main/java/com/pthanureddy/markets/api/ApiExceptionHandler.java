package com.pthanureddy.markets.api;

import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> invalidBody(MethodArgumentNotValidException exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest()
                .body(new ApiError("VALIDATION_ERROR", "Request validation failed", Instant.now(), fields));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiError> invalidPath(ConstraintViolationException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiError(
                        "VALIDATION_ERROR",
                        exception.getMessage(),
                        Instant.now(),
                        Map.of()));
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ApiError> inconsistentState(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError(
                        "INCONSISTENT_STATE",
                        exception.getMessage(),
                        Instant.now(),
                        Map.of()));
    }
}

