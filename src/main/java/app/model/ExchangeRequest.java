package app.model;

import java.math.BigDecimal;

public class ExchangeRequest {
    private final Account from;
    private final Account to;
    private final BigDecimal amount;

    public ExchangeRequest(Account from, Account to, BigDecimal amount) {
        this.from = from;
        this.to = to;
        this.amount = amount;
    }

    public Account getFrom() {
        return from;
    }

    public Account getTo() {
        return to;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return "{" +
                "from=" + from +
                ", to=" + to +
                ", amount=" + amount +
                '}';
    }
}
