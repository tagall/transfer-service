package app.service;

import app.model.Account;
import app.model.CompositeResponse;
import app.model.ExchangeRequest;

public interface DAOService {
    CompositeResponse getAccount(int id);

    CompositeResponse getAllAccounts();

    CompositeResponse insertAccount(Account account);

    CompositeResponse drop();

    CompositeResponse performExchange(ExchangeRequest exchangeRequest);
}
