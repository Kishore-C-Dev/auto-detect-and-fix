package com.example.autodetectandfix.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler that catches all exceptions and logs them with full stack traces.
 * This ensures that exceptions are properly logged to the log file for detection.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<Map<String, Object>> handleNullPointer(NullPointerException ex) {
        logger.error("NullPointerException occurred", ex);
        return buildErrorResponse("Null pointer error", ex, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ArithmeticException.class)
    public ResponseEntity<Map<String, Object>> handleArithmetic(ArithmeticException ex) {
        logger.error("ArithmeticException occurred", ex);
        return buildErrorResponse("Arithmetic error", ex, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ArrayIndexOutOfBoundsException.class)
    public ResponseEntity<Map<String, Object>> handleArrayIndex(ArrayIndexOutOfBoundsException ex) {
        logger.error("ArrayIndexOutOfBoundsException occurred", ex);
        return buildErrorResponse("Array index error", ex, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        logger.error("IllegalArgumentException occurred", ex);
        return buildErrorResponse("Invalid argument", ex, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, Object>> handleIOException(IOException ex) {
        logger.error("IOException occurred", ex);
        return buildErrorResponse("I/O error", ex, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        logger.error("Unexpected error occurred", ex);
        return buildErrorResponse("Internal server error", ex, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            String message, Exception ex, HttpStatus status) {

        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("message", message);
        error.put("exception", ex.getClass().getName());
        error.put("details", ex.getMessage());
        error.put("status", status.value());

        return new ResponseEntity<>(error, status);
    }
}
