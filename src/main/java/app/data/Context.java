package app.data;

import app.service.DAOService;
import app.service.RestService;

public class Context {
    private final DAOService daoService;
    private final RestService restService;

    public Context(DAOService daoService, RestService restService) {
        this.daoService = daoService;
        this.restService = restService;
    }

    public DAOService getDaoService() {
        return daoService;
    }

    public RestService getRestService() {
        return restService;
    }

}
