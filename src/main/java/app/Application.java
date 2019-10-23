package app;

import app.data.Context;
import app.database.DBConnector;
import app.service.DAOServiceImpl;
import app.service.DAOService;
import app.service.RestService;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class Application {
    private static Context context = null;

    static void start() {
        initProperties();
        if (context == null) {
            DAOService daoService = new DAOServiceImpl(new DBConnector());
            context = new Context(daoService, new RestService(daoService));
        }
    }

    public static void initProperties() {
        try {
            ClassLoader classLoader = Application.class.getClassLoader();
            InputStream inputStream = classLoader.getResourceAsStream("app.properties");
            Properties prop = new Properties();
            prop.load(inputStream);
            System.setProperty("db.uri", prop.getProperty("db.uri"));
            System.setProperty("db.user", prop.getProperty("db.user"));
            System.setProperty("db.password", prop.getProperty("db.password"));
            System.setProperty("app.name", prop.getProperty("app.name"));
            System.setProperty("service.port", prop.getProperty("service.port"));
            System.setProperty("service.host", prop.getProperty("service.host"));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Context getContext() {
        return context;
    }
}
