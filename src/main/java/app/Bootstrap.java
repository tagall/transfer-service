package app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bootstrap {

    private static Logger LOG = LoggerFactory.getLogger(Bootstrap.class);

    public static void main(String[] args) {
        start();
        LOG.info("Application {} started", System.getProperty("app.name"));
    }

    private static void start() {
        Application.start();
    }
}
