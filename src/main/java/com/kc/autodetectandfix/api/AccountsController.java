package com.kc.autodetectandfix.api;

import com.kc.autodetectandfix.model.Account;
import com.kc.autodetectandfix.service.AccountsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller for accounts management.
 * Demonstrates the complete error detection and AI fix flow.
 */
@RestController
@RequestMapping("/api/accounts")
public class AccountsController {

    private static final Logger logger = LoggerFactory.getLogger(AccountsController.class);

    private final AccountsService accountsService;

    public AccountsController(AccountsService accountsService) {
        this.accountsService = accountsService;
    }

    /**
     * GET /api/accounts
     * Fetches all user accounts from the external banking API.
     *
     * This endpoint demonstrates:
     * 1. Calling an external API
     * 2. Logging the complete transaction flow
     * 3. Encountering a parsing error (balance type mismatch)
     * 4. Error being detected by the monitoring system
     * 5. AI analyzing and suggesting a fix
     * 6. Email notification with detailed error information
     *
     * @return List of accounts
     */
    @GetMapping
    public ResponseEntity<List<Account>> getAccounts() {
        logger.info("=== GET /api/accounts - Starting account fetch request ===");
        logger.debug("Initiating account retrieval from external banking system");

        try {
            List<Account> accounts = accountsService.fetchAccounts();

            logger.info("Successfully retrieved {} accounts for user", accounts.size());
            logger.debug("Account IDs: {}", accounts.stream()
                .map(Account::getAccountId)
                .toList());

            logger.info("=== GET /api/accounts - Request completed successfully ===");
            return ResponseEntity.ok(accounts);

        } catch (Exception e) {
            logger.error("=== GET /api/accounts - Request failed with error ===");
            logger.error("Error type: {}", e.getClass().getSimpleName());
            logger.error("Error message: {}", e.getMessage());
            throw e; // Re-throw to be caught by GlobalExceptionHandler
        }
    }
}
