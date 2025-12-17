package com.kc.autodetectandfix.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a bank account entity with realistic banking data.
 */
public class Account {

    private Long accountId;
    private String accountNumber;
    private String holderName;
    private AccountType type;
    private BigDecimal balance;
    private String currency;
    private AccountStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdDate;

    public Account() {
    }

    public Account(Long accountId, String accountNumber, String holderName, AccountType type,
                   BigDecimal balance, String currency, AccountStatus status, LocalDateTime createdDate) {
        this.accountId = accountId;
        this.accountNumber = accountNumber;
        this.holderName = holderName;
        this.type = type;
        this.balance = balance;
        this.currency = currency;
        this.status = status;
        this.createdDate = createdDate;
    }

    // Getters and Setters
    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getHolderName() {
        return holderName;
    }

    public void setHolderName(String holderName) {
        this.holderName = holderName;
    }

    public AccountType getType() {
        return type;
    }

    public void setType(AccountType type) {
        this.type = type;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    /**
     * Account type enumeration.
     */
    public enum AccountType {
        SAVINGS,
        CHECKING,
        CREDIT
    }

    /**
     * Account status enumeration.
     */
    public enum AccountStatus {
        ACTIVE,
        INACTIVE,
        SUSPENDED
    }
}
