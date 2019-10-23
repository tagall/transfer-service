package app.model;

import java.util.List;

public class CompositeResponse {
    private final Account account;
    private final List<Account> accounts;
    private final String message;
    private final Boolean error;

    public CompositeResponse(Account account, List<Account> accounts, String message, Boolean error) {
        this.account = account;
        this.accounts = accounts;
        this.message = message;
        this.error = error;
    }

    public Account getAccount() {
        return account;
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public String getMessage() {
        return message;
    }

    public Boolean getError() {
        return error;
    }
}
