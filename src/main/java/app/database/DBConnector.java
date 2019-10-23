package app.database;

import org.h2.jdbcx.JdbcConnectionPool;
import org.h2.tools.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class DBConnector {
    private static Logger LOG = LoggerFactory.getLogger(DBConnector.class);
    JdbcConnectionPool cp;
    private String uri = System.getProperty("db.uri");
    private String user = System.getProperty("db.user");
    private String password = System.getProperty("db.password");

    {
        try {
            Class.forName("org.h2.Driver");
            Server.createTcpServer().start();
            cp = JdbcConnectionPool.create(uri, user, password);
            cp.setMaxConnections(20);
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getDbConnection() throws SQLException {
        Connection connection = cp.getConnection();
        connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
        return connection;
    }
}
