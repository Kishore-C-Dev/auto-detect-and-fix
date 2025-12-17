package com.kc.autodetectandfix.model;

import java.util.List;

/**
 * Response DTO for external banking API.
 * Represents the structure of data returned from mock API.
 */
public class ExternalAccountResponse {

    private String status;
    private String message;
    private AccountData data;

    public ExternalAccountResponse() {
    }

    public ExternalAccountResponse(String status, String message, AccountData data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public AccountData getData() {
        return data;
    }

    public void setData(AccountData data) {
        this.data = data;
    }

    /**
     * Inner class representing the data section of the API response.
     */
    public static class AccountData {
        private List<Account> accounts;
        private Integer totalCount;
        private Integer page;
        private Integer pageSize;

        public AccountData() {
        }

        public AccountData(List<Account> accounts, Integer totalCount, Integer page, Integer pageSize) {
            this.accounts = accounts;
            this.totalCount = totalCount;
            this.page = page;
            this.pageSize = pageSize;
        }

        public List<Account> getAccounts() {
            return accounts;
        }

        public void setAccounts(List<Account> accounts) {
            this.accounts = accounts;
        }

        public Integer getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(Integer totalCount) {
            this.totalCount = totalCount;
        }

        public Integer getPage() {
            return page;
        }

        public void setPage(Integer page) {
            this.page = page;
        }

        public Integer getPageSize() {
            return pageSize;
        }

        public void setPageSize(Integer pageSize) {
            this.pageSize = pageSize;
        }
    }
}
