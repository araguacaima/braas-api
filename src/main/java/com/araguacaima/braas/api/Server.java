package com.araguacaima.braas.api;

import com.araguacaima.braas.api.common.Commons;
import com.araguacaima.braas.api.routes.Admin;
import com.araguacaima.braas.api.routes.Api;
import com.araguacaima.braas.api.routes.Braas;
import com.araguacaima.commons.utils.MapUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.neuland.jade4j.JadeConfiguration;
import de.neuland.jade4j.template.TemplateLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.route.HttpMethod;
import spark.template.jade.JadeTemplateEngine;

import javax.servlet.MultipartConfigElement;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.araguacaima.braas.api.common.Commons.*;
import static com.araguacaima.braas.api.common.Security.setCORS;
import static spark.Spark.*;

@SuppressWarnings("UnstableApiUsage")
public class Server {

    public static JadeConfiguration config = new JadeConfiguration();
    public static JadeTemplateEngine engine = new JadeTemplateEngine(config);
    public static int assignedPort;
    public static String deployedServer;
    public static String basePath;
    public static MultipartConfigElement multipartConfigElement;
    public static Map<String, String> environment;
    private static TemplateLoader templateLoader = new Loader("web/views");
    private static Logger log = LoggerFactory.getLogger(Server.class);
    private static ProcessBuilder processBuilder = new ProcessBuilder();

    static {
        environment = new HashMap<>(processBuilder.environment());
        URL url = Server.class.getResource("/config/config.properties");
        Properties properties = new Properties();
        try {
            properties.load(url.openStream());
            Map<String, String> map = MapUtils.fromProperties(properties);
            if (!map.isEmpty()) {
                log.info("Properties taken from config file '" + url.getFile().replace("file:" + File.separator, "") + "'");
                environment.putAll(map);
            } else {
                log.info("Properties taken from system map...");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.trace("Properties: " + environment);
        config.setTemplateLoader(templateLoader);
        ObjectMapper mapper = jsonUtils.getMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        assignedPort = getAssignedPort();
        deployedServer = environment.get("DEPLOYED_SERVER");
        basePath = environment.get("BASE_PATH");
        config.setBasePath(basePath);
        config.getSharedVariables().put("basePath", basePath);
        config.setPrettyPrint(true);
    }

    private static int getAssignedPort() {
        if (environment.get("PORT") != null) {
            return Integer.parseInt(environment.get("PORT"));
        }
        return 4567;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void main(String[] args) {
        exception(Exception.class, exceptionHandler);
        port(assignedPort);
        //secure("deploy/keystore.jks", "password", null, null);
        staticFiles.location("/web/public");
        staticFiles.externalLocation(TEMP_DIR);
        multipartConfigElement = new MultipartConfigElement("/" + TEMP_DIR);
        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Request-Method", "*");
            response.header("Access-Control-Allow-Headers", "*");
            setCORS(request, response);
        });
        options(Commons.DEFAULT_PATH + "*", (request, response) -> {
            setCORS(request, response);
            Object method1 = null;
            Object method2 = null;
            Map<HttpMethod, Map<Commons.InputOutput, Object>> output = setOptionsOutputStructure(method1, method2, HttpMethod.get, HttpMethod.post);
            return getOptions(request, response, output);
        });
        path(Braas.PATH, new Braas());
        path(Api.PATH, new Api());
        path(Api.ApiFile.PATH, new Api.ApiFile());
        //path(Api.ApiGoogleDrive.PATH, new Api.ApiGoogleDrive());
        path(Api.ApiBinary.PATH, new Api.ApiBinary());
        path(Admin.PATH, new Admin());
        log.info("Server listen on port '" + assignedPort + "'");
    }

}

