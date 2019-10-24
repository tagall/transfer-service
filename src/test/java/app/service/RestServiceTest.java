package app.service;

import app.Application;
import app.database.DBConnector;
import app.model.Account;
import app.model.CompositeResponse;
import app.model.ExchangeRequest;
import com.google.gson.Gson;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RestServiceTest {
    DAOService daoService;
    RestService restService;
    private static Logger LOG = LoggerFactory.getLogger(RestServiceTest.class);

    private String CREATE_ACCOUNT = "http://localhost:4567/createAccount";
    private String GET_ACCOUNT = "http://localhost:4567/getAccount/";
    private String GET_ALL_ACCOUNTS = "http://localhost:4567/getAllAccounts";
    private String EXCHANGE = "http://localhost:4567/performExchange";

    /*
        please note
        some of cases has been checked manually using postman and prepared requests
        in case of further maintenance and code improvements
        it is strongly recommended to add tests here
     */

    @BeforeEach
    void init() {
        Application.initProperties();
        daoService = new DAOServiceImpl(new DBConnector());
        restService = new RestService(daoService);
    }

    /*
    TODO: use carefully unless UAT is run of physical DB and test in-memory DB
     */

    @AfterEach
    void cleanUp() {
        daoService.drop();
    }

    @Test
    void checkDeserialization() throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        Account jon = new Account(1, "Jon", BigDecimal.valueOf(100.1));
        createAndExecute(jon, CREATE_ACCOUNT, httpClient);
        Account account = getAccountFromRestForId(1, httpClient);
        assertEquals(account, jon);
        httpClient.close();
    }

    /*
        main goal of this test is to make simultaneous exchange operations which will cause
        update operation for same account
        in that case one of requests have to respond with message like "Can't update" depending on how much rows has been updated
     */
    @Test
    void transferMoneySimultaneous() throws InterruptedException, IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        int accountsCount = 100;
        int initialAmount = 100;
// also checked for 100_000
        int transfersCount = 1000;
        BigDecimal ant = BigDecimal.valueOf(initialAmount);
        Random random = new Random();

        for (int i = 0; i < accountsCount; i++) {
            Account acc = new Account(i, "test" + i, ant);
            createAndExecute(acc, CREATE_ACCOUNT, httpClient);
        }

        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < transfersCount; i++) {
            tasks.add(() -> {
                int from = random.nextInt(accountsCount - 1) + 1;
                int amt = random.nextInt(initialAmount - 1) + 1;
                int to = accountsCount - from + 1;
                HttpUriRequest postRequest = createPostRequestForExchange(getAccountFromRestForId(from, httpClient), getAccountFromRestForId(to, httpClient), EXCHANGE, BigDecimal.valueOf(amt));
                CloseableHttpResponse response = httpClient.execute(postRequest);
                handleResponse(response);
                return 0;
            });
        }
        executorService.invokeAll(tasks);
        List<Account> allAccounts = getAllAccounts(httpClient);
        BigDecimal reduce = allAccounts.stream().map(Account::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
        LOG.debug("Total amount {} ", reduce);
        Assertions.assertEquals(reduce, BigDecimal.valueOf(initialAmount * accountsCount));
    }

    private void createAndExecute(Account jon, String uri, CloseableHttpClient httpClient) throws IOException {
        HttpUriRequest request = createPostRequestForAccount(jon, uri);
        CloseableHttpResponse response = httpClient.execute(request);
        handleResponse(response);
    }

    private void handleResponse(CloseableHttpResponse response) throws IOException {
        try {
            EntityUtils.consume(response.getEntity());
        } finally {
            response.close();
        }
    }

    private Account getAccountFromRestForId(int id, CloseableHttpClient httpClient) throws IOException {
        HttpUriRequest request = new HttpGet(GET_ACCOUNT + id);
        CloseableHttpResponse response = httpClient.execute(request);
        HttpEntity entity = response.getEntity();
        CompositeResponse compositeResponse = new Gson().fromJson(EntityUtils.toString(entity), CompositeResponse.class);
        Account account = compositeResponse.getAccount();
        handleResponse(response);
        return account;
    }

    private List<Account> getAllAccounts(CloseableHttpClient httpClient) throws IOException {
        HttpUriRequest request = new HttpGet(GET_ALL_ACCOUNTS);
        CloseableHttpResponse response = httpClient.execute(request);
        HttpEntity entity = response.getEntity();
        CompositeResponse compositeResponse = new Gson().fromJson(EntityUtils.toString(entity), CompositeResponse.class);
        handleResponse(response);
        return compositeResponse.getAccounts();
    }

    @Test
    void checkTransfer() throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        Account jon = new Account(1, "Jon", BigDecimal.valueOf(100));
        Account snow = new Account(2, "Snow", BigDecimal.valueOf(100));
        createAndExecute(jon, CREATE_ACCOUNT, httpClient);
        createAndExecute(snow, CREATE_ACCOUNT, httpClient);

        //makeExchange
        BigDecimal amt = BigDecimal.valueOf(30);
        HttpUriRequest postRequest = createPostRequestForExchange(jon, snow, EXCHANGE, amt);
        CloseableHttpResponse response = httpClient.execute(postRequest);
        handleResponse(response);

        //check Jon
        Account accountJon = getAccountFromRestForId(1, httpClient);
        assertEquals(accountJon.getBalance(), jon.getBalance().subtract(amt));

        //check Snow
        Account accountSnow = getAccountFromRestForId(2, httpClient);
        assertEquals(accountSnow.getBalance(), snow.getBalance().add(amt));
    }

    void checkOppositeTransfer() throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        Account jon = new Account(1, "Jon", BigDecimal.valueOf(100));
        Account snow = new Account(2, "Snow", BigDecimal.valueOf(100));
        createAndExecute(jon, CREATE_ACCOUNT, httpClient);
        createAndExecute(snow, CREATE_ACCOUNT, httpClient);

        //makeExchange
        BigDecimal amt = BigDecimal.valueOf(100);
        HttpUriRequest postRequest = createPostRequestForExchange(jon, snow, EXCHANGE, amt);
        CloseableHttpResponse response = httpClient.execute(postRequest);
        handleResponse(response);

        //check Jon
        Account accountJon = getAccountFromRestForId(1, httpClient);
        assertEquals(accountJon.getBalance(), jon.getBalance().subtract(amt));

        //check Snow
        Account accountSnow = getAccountFromRestForId(2, httpClient);
        assertEquals(accountSnow.getBalance(), snow.getBalance().add(amt));
    }

    private HttpUriRequest createPostRequestForAccount(Account account, String uri) {
        return RequestBuilder.create("POST")
                .setUri(uri)
                .setEntity(new StringEntity(new Gson().toJson(account), ContentType.APPLICATION_JSON))
                .build();
    }

    private HttpUriRequest createPostRequestForExchange(Account accountFrom, Account accountTo, String uri, BigDecimal amount) {
        return RequestBuilder.create("POST")
                .setUri(uri)
                .setEntity(new StringEntity(new Gson().toJson(new ExchangeRequest(accountFrom, accountTo, amount)), ContentType.APPLICATION_JSON))
                .build();
    }
}