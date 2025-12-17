package com.kc.autodetectandfix.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for triggering test exceptions to demonstrate the error detection system.
 */
@RestController
@RequestMapping("/api/test")
public class TestController {

    private static final Logger logger = LoggerFactory.getLogger(TestController.class);

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "Auto Detect and Fix is running");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/null-pointer")
    public ResponseEntity<?> triggerNullPointer() {
        logger.info("Triggering NullPointerException test");
        String str = null;
        // This will cause a NullPointerException
        return ResponseEntity.ok(str.length());
    }

    @GetMapping("/arithmetic")
    public ResponseEntity<?> triggerArithmetic() {
        logger.info("Triggering ArithmeticException test");
        int result = 10 / 0;  // Division by zero
        return ResponseEntity.ok(result);
    }

    @GetMapping("/array-index")
    public ResponseEntity<?> triggerArrayIndex() {
        logger.info("Triggering ArrayIndexOutOfBoundsException test");
        int[] arr = {1, 2, 3};
        return ResponseEntity.ok(arr[10]);  // Index out of bounds
    }

    @PostMapping("/validation")
    public ResponseEntity<?> triggerValidation(@RequestBody Map<String, Object> data) {
        logger.info("Triggering validation error test");
        if (!data.containsKey("required-field")) {
            throw new IllegalArgumentException("Missing required field: required-field");
        }
        return ResponseEntity.ok(data);
    }

    @GetMapping("/io-error")
    public ResponseEntity<?> triggerIOError() throws IOException {
        logger.info("Triggering IOException test");
        // Simulate file not found
        throw new IOException("File not found: /missing/file.txt");
    }

    @GetMapping("/config-error")
    public ResponseEntity<?> triggerConfigError() {
        logger.info("Triggering configuration error test");
        throw new RuntimeException("Missing configuration property: app.database.url in application.yml");
    }

    @GetMapping("/data-error")
    public ResponseEntity<?> triggerDataError() {
        logger.info("Triggering data validation error test");
        throw new IllegalArgumentException("Data validation failed: Email format is invalid");
    }

    @GetMapping("/infra-error")
    public ResponseEntity<?> triggerInfraError() {
        logger.info("Triggering infrastructure error test");
        throw new RuntimeException("java.net.ConnectException: Connection refused to database server at localhost:5432");
    }
}
