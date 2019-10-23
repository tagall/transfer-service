package app.service;

import app.database.DBConnector;
import app.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;

public class DAOServiceImpl implements DAOService {
    private static Logger LOG = LoggerFactory.getLogger(DAOServiceImpl.class);
    private final DBConnector dbConnector;

    //language=H2
    private static final String CREATE_ACCOUNT_TABLE = "CREATE TABLE ACCOUNTS (ID INT PRIMARY KEY AUTO_INCREMENT, NAME VARCHAR(255), BALANCE VARCHAR(255))";
    private static final String DROP_ACCOUNT_TABLE = "DROP TABLE ACCOUNTS";
    private static final String INSERT_INTO_ACCOUNT_TABLE = "INSERT INTO ACCOUNTS (NAME, BALANCE) VALUES(?,?)";
    private static final String SELECT_ALL_FROM_ACCOUNT_TABLE = "SELECT * FROM ACCOUNTS";
    private static final String SELECT_ACCOUNT_FROM_ACCOUNT_TABLE_BY_ID = "SELECT * FROM ACCOUNTS WHERE ID = ?";
    private static final String EXCHANGE_SQL = "UPDATE ACCOUNTS \n" +
            "SET BALANCE = \n" +
            "    CASE " +
            "    WHEN ID = ? THEN ?\n" +
            "    WHEN ID = ? THEN ?\n" +
            "    ELSE BALANCE \n" +
            "    END\n" +
            "WHERE (ID = ? AND BALANCE = ?) OR (ID = ? AND BALANCE = ?)";

    public DAOServiceImpl(DBConnector dbConnector) {
        this.dbConnector = dbConnector;
        // for tasks sake
        try {
            initScheme();
        } catch (SQLException e) {
            LOG.error("Can't initialize primitive app.database.", e);
            e.printStackTrace();
        }
    }

    private void initScheme() throws SQLException {
        executeForCreate(CREATE_ACCOUNT_TABLE);
    }

    @Override
    public CompositeResponse getAccount(int id) {
        Account account = null;
        CompositeResponseBuilder compositeResponseBuilder = new CompositeResponseBuilder().setError(false);
        try {
            Connection dbConnection = dbConnector.getDbConnection();
            PreparedStatement preparedStatement = dbConnection.prepareStatement(SELECT_ACCOUNT_FROM_ACCOUNT_TABLE_BY_ID);
            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                account = new Account(
                        resultSet.getInt(1),
                        resultSet.getString(2),
                        resultSet.getBigDecimal(3));
            }
            dbConnection.close();
        } catch (SQLException e) {
            e.printStackTrace();
            prepareUnsuccessfulResponse(compositeResponseBuilder, "Error in SQL execution");
        }
        if (account == null) {
            prepareUnsuccessfulResponse(compositeResponseBuilder, "No accounts found for id " + id);
        }
        return compositeResponseBuilder.setAccount(account).createCompositeResponse();
    }


    @Override
    public CompositeResponse getAllAccounts() {
        ArrayList<Account> accounts = new ArrayList<>();
        CompositeResponseBuilder compositeResponseBuilder = new CompositeResponseBuilder().setError(false);
        try (Connection dbConnection = dbConnector.getDbConnection(); Statement statement = dbConnection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(SELECT_ALL_FROM_ACCOUNT_TABLE);
            while (resultSet.next()) {
                accounts.add(new Account(
                        resultSet.getInt(1),
                        resultSet.getString(2),
                        resultSet.getBigDecimal(3)));
            }
        } catch (SQLException e) {
            prepareUnsuccessfulResponse(compositeResponseBuilder, "Error in SQL execution");
        }
        if (accounts.isEmpty()) {
            prepareUnsuccessfulResponse(compositeResponseBuilder, "No accounts in system yet.");
        }
        return compositeResponseBuilder.setAccounts(accounts).createCompositeResponse();
    }

    @Override
    public CompositeResponse insertAccount(Account account) {
        CompositeResponseBuilder compositeResponseBuilder = new CompositeResponseBuilder().setError(false);
        try (Connection dbConnection = dbConnector.getDbConnection()) {
            try (PreparedStatement preparedStatement = dbConnection.prepareStatement(INSERT_INTO_ACCOUNT_TABLE)) {
                preparedStatement.setString(1, account.getName());
                preparedStatement.setBigDecimal(2, account.getBalance());
                if (preparedStatement.executeUpdate() == 0) {
                    prepareUnsuccessfulResponse(compositeResponseBuilder, "Couldn't insert" + account);
                    return compositeResponseBuilder.createCompositeResponse();
                }
                compositeResponseBuilder.setMessage("Account was created");
            }
        } catch (SQLException e) {
            prepareUnsuccessfulResponse(compositeResponseBuilder, "Error in SQL execution");
            e.printStackTrace();
        }
        return compositeResponseBuilder.createCompositeResponse();
    }

    private void executeForCreate(String sql) {
        try (Connection dbConnection = dbConnector.getDbConnection(); Statement statement = dbConnection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            LOG.error("Error in SQL execution");
            e.printStackTrace();
        }
    }

    @Override
    public CompositeResponse drop() {
        CompositeResponseBuilder compositeResponseBuilder = new CompositeResponseBuilder().setError(false);
        try (Connection dbConnection = dbConnector.getDbConnection(); Statement statement = dbConnection.createStatement()) {
            statement.execute(DROP_ACCOUNT_TABLE);
        } catch (SQLException e) {
            prepareUnsuccessfulResponse(compositeResponseBuilder, "Error in SQL execution");
            e.printStackTrace();
        }
        return compositeResponseBuilder.createCompositeResponse();
    }


    @Override
    public CompositeResponse performExchange(ExchangeRequest exchangeRequest) {
        CompositeResponseBuilder compositeResponseBuilder = new CompositeResponseBuilder().setError(false);
        try {
            CompositeResponse fromCR = getAccount(exchangeRequest.getFrom().getId());
            CompositeResponse toCR = getAccount(exchangeRequest.getTo().getId());
            if (fromCR.getError() || toCR.getError()) {
                prepareUnsuccessfulResponse(compositeResponseBuilder, fromCR.getError() ? fromCR.getMessage() : toCR.getMessage());
                return compositeResponseBuilder.createCompositeResponse();
            }

            Account from = fromCR.getAccount();
            Account to = toCR.getAccount();
            BigDecimal amount = exchangeRequest.getAmount();

            if (amount.compareTo(from.getBalance()) < 0) {
                Connection dbConnection = dbConnector.getDbConnection();
                LOG.debug("Transfer {} amount from {} to {}", amount, from.getName(), to.getName());
                PreparedStatement preparedStatement = dbConnection.prepareStatement(EXCHANGE_SQL);
                preparedStatement.setInt(1, from.getId());
                preparedStatement.setBigDecimal(2, from.getBalance().subtract(amount));
                preparedStatement.setInt(3, to.getId());
                preparedStatement.setBigDecimal(4, to.getBalance().add(amount));
                preparedStatement.setInt(5, from.getId());
                preparedStatement.setBigDecimal(6, from.getBalance());
                preparedStatement.setInt(7, to.getId());
                preparedStatement.setBigDecimal(8, to.getBalance());
                int i = preparedStatement.executeUpdate();
                dbConnection.commit();
                dbConnection.close();
                if (i == 0) {
                    prepareUnsuccessfulResponse(compositeResponseBuilder, "Update unsuccessful - please try again");
                    return compositeResponseBuilder.createCompositeResponse();
                }
            } else {
                prepareUnsuccessfulResponse(compositeResponseBuilder, "Not enough money to perform operation for "
                        + from.getName() + ". Requested amount - " + amount);
                return compositeResponseBuilder.createCompositeResponse();
            }
            compositeResponseBuilder.setMessage("Exchanged successfully");
        } catch (SQLException e) {
            prepareUnsuccessfulResponse(compositeResponseBuilder, "Couldn't perform exchange for request: " + exchangeRequest.toString());
            e.printStackTrace();
        }
        return compositeResponseBuilder.createCompositeResponse();
    }

    private void prepareUnsuccessfulResponse(CompositeResponseBuilder compositeResponseBuilder, String message) {
        compositeResponseBuilder.setError(true).setMessage(message);
        LOG.warn(message);
    }
}
