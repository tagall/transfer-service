package app.model;

import java.util.List;

public class CompositeResponseBuilder {
    private Account account;
    private List<Account> accounts;
    private String message;
    private Boolean error;

    public CompositeResponseBuilder setAccount(Account account) {
        this.account = account;
        return this;
    }

    public CompositeResponseBuilder setAccounts(List<Account> accounts) {
        this.accounts = accounts;
        return this;
    }

    public CompositeResponseBuilder setMessage(String message) {
        this.message = message;
        return this;
    }

    public CompositeResponseBuilder setError(Boolean error) {
        this.error = error;
        return this;
    }

    public CompositeResponse createCompositeResponse() {
        return new CompositeResponse(account, accounts, message, error);
    }
}