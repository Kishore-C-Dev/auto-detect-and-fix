package com.kc.autodetectandfix.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * Mock external banking API controller.
 * Simulates a third-party banking API that returns account data.
 * Intentionally returns balance as STRING to trigger parsing errors.
 */
@RestController
@RequestMapping("/api/mock")
public class MockBankingApiController {

    private static final Logger logger = LoggerFactory.getLogger(MockBankingApiController.class);

    @GetMapping("/accounts")
    public ResponseEntity<Map<String, Object>> getMockAccounts() throws InterruptedException {
        logger.info("Mock Banking API: Received request for accounts");

        // Simulate network delay
        Thread.sleep(500);

        // Build response manually as Map to control JSON types
        // CRITICAL: balance fields are STRINGS, not numbers - this will cause parsing error
        Map<String, Object> account1 = new HashMap<>();
        account1.put("accountId", 101);
        account1.put("accountNumber", "ACC-2025-001");
        account1.put("holderName", "John Doe");
        account1.put("type", "SAVINGS");
        account1.put("balance", "5000.50");  // STRING instead of number!
        account1.put("currency", "USD");
        account1.put("status", "ACTIVE");
        account1.put("createdDate", "2024-12-15T10:30:00");

        Map<String, Object> account2 = new HashMap<>();
        account2.put("accountId", 102);
        account2.put("accountNumber", "ACC-2025-002");
        account2.put("holderName", "Jane Smith");
        account2.put("type", "CHECKING");
        account2.put("balance", "12500.75");  // STRING instead of number!
        account2.put("currency", "USD");
        account2.put("status", "ACTIVE");
        account2.put("createdDate", "2024-06-15T14:20:00");

        Map<String, Object> account3 = new HashMap<>();
        account3.put("accountId", 103);
        account3.put("accountNumber", "ACC-2025-003");
        account3.put("holderName", "Bob Johnson");
        account3.put("type", "CREDIT");
        account3.put("balance", "3250.00");  // STRING instead of number!
        account3.put("currency", "USD");
        account3.put("status", "ACTIVE");
        account3.put("createdDate", "2024-09-15T09:15:00");

        Map<String, Object> data = new HashMap<>();
        data.put("accounts", List.of(account1, account2, account3));
        data.put("totalCount", 3);
        data.put("page", 1);
        data.put("pageSize", 10);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Accounts retrieved successfully");
        response.put("data", data);

        logger.info("Mock Banking API: Returning 3 accounts with STRING balance values (will cause parsing error)");

        return ResponseEntity.ok(response);
    }
}
