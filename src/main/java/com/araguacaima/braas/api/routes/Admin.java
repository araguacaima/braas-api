package com.araguacaima.braas.api.routes;

import com.araguacaima.braas.api.BeanBuilder;
import com.araguacaima.braas.api.common.Commons;
import com.araguacaima.braas.api.filter.SessionFilter;
import com.araguacaima.braas.core.google.model.Account;
import com.araguacaima.braas.core.google.model.Role;
import org.apache.commons.collections4.IterableUtils;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.sparkjava.SparkWebContext;
import spark.RouteGroup;

import java.util.*;

import static com.araguacaima.braas.api.Server.engine;
import static com.araguacaima.braas.api.common.Commons.*;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_OK;
import static spark.Spark.*;

public class Admin implements RouteGroup {

    public static final String PATH = "/admin";

    @Override
    public void addRoutes() {
        //before(Commons.EMPTY_PATH, Commons.genericFilter, Commons.adminApiFilter);
        //before("/*", Commons.adminApiFilter);
        ArrayList<String> header = new ArrayList<>(Arrays.asList("Enabled", "Login", "Email"));
        header.addAll(Commons.ALL_ROLES);
        List<Account> accounts = new ArrayList<>();
        get(Commons.EMPTY_PATH, buildRoute(new BeanBuilder()
                .title("Braas Admin")
                .accounts(accounts)
                .roles(Commons.ALL_ROLES)
                .header(header), Admin.PATH), engine);
        patch(Commons.EMPTY_PATH, (request, response) -> {
            try {
                Map requestInput = jsonUtils.fromJSON(request.body(), Map.class);
                String email = (String) requestInput.get("email");
                boolean approved = (Boolean) requestInput.get("approved");
                String role = (String) requestInput.get("role");
                Map<String, Object> params = new HashMap<>();
                params.put(Account.PARAM_EMAIL, email);
                Account account = null;
                if (account != null) {
                    Set<Role> roles = account.getRoles();
                    Map<String, Object> roleParams = new HashMap<>();
                    roleParams.put(Role.PARAM_NAME, role);
                    Role role_ = null;
                    if (role_ == null) {
                        throw halt("Role does not exists");
                    } else {
                        Role innerRole = IterableUtils.find(roles, role1 -> role1.getName().equals(role));
                        SparkWebContext context = new SparkWebContext(request, response);
                        CommonProfile profile = Commons.findAndFulfillProfile(context);
                        SessionFilter.SessionMap map = SessionFilter.map.get(email);
                        if (map != null) {
                            map.setActive(false);
                        }
                        if (approved) {
                            if (innerRole == null) {
                                roles.add(role_);
                                profile.addRole(role);
                            }
                        } else {
                            if (innerRole != null) {
                                roles.remove(innerRole);
                            }
                        }
                    }
                    response.status(HTTP_OK);
                    return EMPTY_RESPONSE;
                } else {
                    response.status(HTTP_BAD_REQUEST);
                    return EMPTY_RESPONSE;
                }
            } catch (Throwable ex) {
                ex.printStackTrace();
                response.status(HTTP_BAD_REQUEST);
                response.type(JSON_CONTENT_TYPE);
                return EMPTY_RESPONSE;
            }
        });

    }

}
