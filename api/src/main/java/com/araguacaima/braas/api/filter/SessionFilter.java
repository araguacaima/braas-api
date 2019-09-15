package com.araguacaima.braas.api.filter;

import com.araguacaima.braas.api.common.Commons;
import com.araguacaima.braas.api.routes.Braas;
import com.araguacaima.braas.core.google.model.Account;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.sparkjava.SparkWebContext;
import spark.Filter;
import spark.Request;
import spark.Response;
import spark.Session;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static spark.Spark.redirect;

/**
 * Servlet Filter implementation class SessionFilter
 */

public class SessionFilter implements Filter {

    public static Map<String, SessionMap> map = new HashMap<>();

    public void handle(Request request, Response response) throws IOException {
        SparkWebContext context = new SparkWebContext(request, response);
        CommonProfile profile = Commons.findAndFulfillProfile(context);
        if (profile != null) {
            Account account = (Account) context.getSessionAttribute("account");
            if (account == null) {
                Map<String, Object> param = new HashMap<>();
                param.put(Account.PARAM_EMAIL, profile.getEmail());

            }
            if (account != null) {
                String email = account.getEmail();
                SessionMap sessionMap = map.get(email);
                if (sessionMap != null) {
                    if (!sessionMap.isActive()) {
                        sessionMap.getSession().invalidate();
                        redirect.get("", Braas.PATH);
                        map.remove(email);
                        account = null;
                    }
                } else {
                    sessionMap = new SessionMap(request.session(), true);
                    SessionFilter.map.put(email, sessionMap);
                }
                context.setSessionAttribute("account", account);
            }
        }
    }

    public static class SessionMap {
        private Session session;
        private boolean active;

        public SessionMap(Session session, boolean active) {
            this.session = session;
            this.active = active;
        }

        public Session getSession() {
            return session;
        }

        public void setSession(Session session) {
            this.session = session;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }
    }
}