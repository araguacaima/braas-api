package com.araguacaima.braas.api.routes;

import com.araguacaima.braas.api.BeanBuilder;
import org.apache.commons.lang3.StringUtils;
import spark.RouteGroup;

import static com.araguacaima.braas.api.Server.engine;
import static com.araguacaima.braas.api.common.Commons.*;
import static spark.Spark.get;

public class Braas implements RouteGroup {

    public static final String PATH = "/";

    @Override
    public void addRoutes() {
        //get(StringUtils.EMPTY, buildRoute(new BeanBuilder().title(BRAAS), "/home"), engine);
        get(StringUtils.EMPTY, buildRoute(new BeanBuilder()
                .title(BRAAS)
                .product(PRODUCT)
                .shortDescription(PRODUCT_DESCRIPTION), "/index"), engine);
/*        before("/login/google", Commons.scopesFilter);
        get("/login/google", Authentication.authGoogle, engine);
        get("/login", Authentication.login, engine);
        get("/callback", Commons.callback);
        post("/callback", (req, res) -> {
            store(req, res);
            return Commons.callback.handle(req, res);
        });*/


        /*final LogoutRoute localLogout = new LogoutRoute(config, "/");
        localLogout.setDestroySession(true);
        localLogout.setLocalLogout(false);
        localLogout.setCentralLogout(true);
        get("/logout", localLogout);
        final LogoutRoute centralLogout = new LogoutRoute(config);
        centralLogout.setDefaultUrl("http://" + deployedServer + "");
        centralLogout.setLogoutUrlPattern("http://" + deployedServer + "/.*");
        centralLogout.setLocalLogout(false);
        centralLogout.setCentralLogout(true);
        centralLogout.setDestroySession(true);
        get("/central-logout", centralLogout);
        get("/force-login", (rq, rs) -> forceLogin(config, rq, rs));

        get("/jwt", Authentication::jwt, engine);
        get("/login", (rq, rs) -> form(Commons.config), engine);*/


    }

}
