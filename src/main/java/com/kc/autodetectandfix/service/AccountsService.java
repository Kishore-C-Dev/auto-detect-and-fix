package com.kc.autodetectandfix.service;

import com.kc.autodetectandfix.config.AccountsConfig;
import com.kc.autodetectandfix.model.Account;
import com.kc.autodetectandfix.model.ExternalAccountResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for fetching account data from external banking API.
 * This service intentionally has a parsing error to demonstrate the error detection system.
 */
@Service
public class AccountsService {

    private static final Logger logger = LoggerFactory.getLogger(AccountsService.class);

    private final WebClient webClient;
    private final AccountsConfig config;
    private final ObjectMapper objectMapper;

    public AccountsService(@Qualifier("accountsWebClient") WebClient webClient,
                          AccountsConfig config,
                          ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.config = config;
        this.objectMapper = objectMapper;
    }

    /**
     * Fetches accounts from external banking API.
     * Contains intentional parsing error for demonstration purposes.
     *
     * @return List of accounts
     */
    public List<Account> fetchAccounts() {
        logger.info("Starting account fetch from external banking API");
        logger.debug("API URL: {}", config.getMockApiUrl());
        logger.debug("Timeout: {} seconds", config.getTimeoutSeconds());

        try {
            // Call external API
            logger.info("Making HTTP request to external banking API...");
            String responseBody = webClient.get()
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .block();

            logger.info("Response received from external API");
            logger.debug("Response body length: {} characters", responseBody != null ? responseBody.length() : 0);

            // Parse the response - THIS IS WHERE THE ERROR WILL OCCUR
            logger.debug("Starting JSON parsing...");
            List<Account> accounts = parseAccountsFromResponse(responseBody);

            logger.info("Successfully parsed {} accounts from external API", accounts.size());
            return accounts;

        } catch (Exception e) {
            logger.error("Error occurred while fetching accounts from external API: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch accounts from external banking API", e);
        }
    }

    /**
     * Parses account data from JSON response.
     * INTENTIONAL ERROR: This method tries to parse balance directly as BigDecimal
     * without handling the case where it might be a String.
     *
     * @param responseBody JSON response from external API
     * @return List of parsed accounts
     */
    private List<Account> parseAccountsFromResponse(String responseBody) throws Exception {
        logger.debug("Parsing account data from JSON response...");

        // Parse the top-level response
        JsonNode rootNode = objectMapper.readTree(responseBody);
        JsonNode accountsArray = rootNode.path("data").path("accounts");

        List<Account> accounts = new ArrayList<>();

        // Iterate through accounts
        for (JsonNode accountNode : accountsArray) {
            logger.debug("Parsing account: {}", accountNode.path("accountNumber").asText());

            Account account = new Account();
            account.setAccountId(accountNode.path("accountId").asLong());
            account.setAccountNumber(accountNode.path("accountNumber").asText());
            account.setHolderName(accountNode.path("holderName").asText());

            // Parse account type
            String typeStr = accountNode.path("type").asText();
            account.setType(Account.AccountType.valueOf(typeStr));

            // CRITICAL BUG: This code assumes balance is always a numeric node
            // If the external API returns balance as a string "5000.50", this will throw an exception
            // because we're calling decimalValue() which requires a numeric node, not a text node
            if (!accountNode.path("balance").isNumber()) {
                throw new IllegalArgumentException(
                    "Invalid balance format for account " + accountNode.path("accountNumber").asText() +
                    ": expected numeric value but got text '" + accountNode.path("balance").asText() + "'"
                );
            }
            BigDecimal balance = accountNode.path("balance").decimalValue();
            account.setBalance(balance);

            account.setCurrency(accountNode.path("currency").asText());

            // Parse account status
            String statusStr = accountNode.path("status").asText();
            account.setStatus(Account.AccountStatus.valueOf(statusStr));

            // Parse created date if present
            if (accountNode.has("createdDate")) {
                String dateStr = accountNode.path("createdDate").asText();
                account.setCreatedDate(LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME));
            } else {
                account.setCreatedDate(LocalDateTime.now());
            }

            accounts.add(account);
            logger.debug("Successfully parsed account: {}", account.getAccountNumber());
        }

        return accounts;
    }
}
