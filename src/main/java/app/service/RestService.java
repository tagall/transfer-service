package app.service;

import app.model.Account;
import app.model.CompositeResponse;
import app.model.CompositeResponseBuilder;
import app.model.ExchangeRequest;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.concurrent.locks.ReentrantLock;

import static spark.Spark.get;
import static spark.Spark.post;

public class RestService {
    ReentrantLock lock = new ReentrantLock();
    private final DAOService daoService;

    public RestService(DAOService daoService) {
        this.daoService = daoService;
        initRouting();
    }

    private void initRouting() {
        post("/createAccount", (request, response) -> {
            response.type("application/json");
            Account account;
            try {
                account = new Gson().fromJson(request.body(), Account.class);
            } catch (Exception e) {
                e.printStackTrace();
                return new Gson().toJsonTree(new CompositeResponseBuilder().setError(true).setMessage(e.getMessage()));
            }
            return new Gson().toJsonTree(daoService.insertAccount(account));
        });

        get("/getAllAccounts", ((request, response) -> {
            response.type("application/json");
            return new Gson().toJsonTree(daoService.getAllAccounts());
        }));

        get("/getAccount/:id", ((request, response) -> {
            response.type("application/json");
            int id;
            try {
                id = Integer.parseInt(request.params(":id"));
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return new Gson().toJson(new CompositeResponseBuilder().setError(true).setMessage("Invalid Number. " + e.getMessage()));
            }
            return new Gson().toJsonTree(daoService.getAccount(id));
        }));


        post("/performExchange", (request, response) -> {
            ExchangeRequest exchangeRequest = new Gson().fromJson(request.body(), ExchangeRequest.class);
            return new Gson().toJsonTree(performExchange(exchangeRequest));
        });
    }

    private CompositeResponse performExchange(ExchangeRequest exchangeRequest) {
        try {
//            no need to use locks here as far H2 handles multithreading well
//            to think up about transactional locks for other DBs
            lock.lock();
            return daoService.performExchange(exchangeRequest);
        } finally {
            lock.unlock();
        }
    }
}
