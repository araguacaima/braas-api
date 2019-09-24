package com.araguacaima.braas.api;

import com.araguacaima.braas.api.common.Commons;
import com.araguacaima.braas.api.routes.Admin;
import com.araguacaima.braas.api.routes.Api;
import com.araguacaima.braas.api.routes.Braas;
import com.araguacaima.commons.utils.FileUtils;
import com.araguacaima.commons.utils.MapUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.neuland.jade4j.JadeConfiguration;
import de.neuland.jade4j.template.TemplateLoader;
import org.apache.commons.lang3.StringUtils;
import org.pac4j.sparkjava.SparkWebContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.route.HttpMethod;
import spark.template.jade.JadeTemplateEngine;

import javax.servlet.MultipartConfigElement;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static com.araguacaima.braas.api.common.Commons.*;
import static com.araguacaima.braas.api.common.Security.setCORS;
import static spark.Spark.*;

@SuppressWarnings("UnstableApiUsage")
public class Server {
    public static final String TEMP_DIR_PARAM = "tmp";
    public static final String UPLOAD_DIR = "upload";
    public static final String RULES_DIR = "rules";
    public static final String SOURCE_CLASSES_DIR = "source-classes";
    public static final String COMPILED_CLASSES_DIR = "compiled-classes";
    public static final String SESSION_ID_PARAM = "sessionId";
    public static final String UPLOAD_DIR_PARAM = "uploadDir";
    public static final String RULES_DIR_PARAM = "rulesDir";
    public static final String SOURCE_CLASSES_DIR_PARAM = "sourceClassesDir";
    public static final String COMPILED_CLASSES_DIR_PARAM = "compiledClassesDir";
    public static JadeConfiguration config = new JadeConfiguration();
    public static JadeTemplateEngine engine = new JadeTemplateEngine(config);
    public static int assignedPort;
    public static String deployedServer;
    public static String basePath;
    public static MultipartConfigElement multipartConfigElement;
    private static TemplateLoader templateLoader = new Loader("web/views");
    private static Logger log = LoggerFactory.getLogger(Server.class);
    private static Map<String, String> environment;
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
    public static void main(String[] args) throws GeneralSecurityException {
        exception(Exception.class, exceptionHandler);
        port(assignedPort);
        //secure("deploy/keystore.jks", "password", null, null);
        staticFiles.location("/web/public");
        staticFiles.externalLocation(TEMP_DIR_PARAM);
        multipartConfigElement = new MultipartConfigElement("/" + TEMP_DIR_PARAM);
        before((request, response) -> {
            String sessionId;
            final SparkWebContext ctx = new SparkWebContext(request, response);
            String storedSessionId = (String) ctx.getSessionAttribute(SESSION_ID_PARAM);
            sessionId = storedSessionId;

            if (StringUtils.isBlank(sessionId)) {
                sessionId = request.cookie("braas-session-id");
                if (StringUtils.isBlank(sessionId)) {
                    sessionId = UUID.randomUUID().toString();
                    response.cookie("braas-session-id", sessionId, 86400, true);
                }
            }

            if (StringUtils.isBlank(sessionId)) {
                sessionId = UUID.randomUUID().toString();
            }
            File tempDir = null;
            if (StringUtils.isNotBlank(storedSessionId)) {
                File baseDir = new File(System.getProperty("java.io.tmpdir"));
                tempDir = new File(baseDir, sessionId);
            }

            if (StringUtils.isBlank(storedSessionId) || tempDir == null || !tempDir.exists()) {
                File baseDir = new File(System.getProperty("java.io.tmpdir"));
                tempDir = new File(baseDir, sessionId);
                if (!tempDir.exists()) {
                    tempDir = FileUtils.createTempDir(sessionId);
                }
                //tempDir.deleteOnExit();
                File uploadPath = new File(tempDir, UPLOAD_DIR);
                uploadPath.mkdir();
                //uploadPath.deleteOnExit();
                File rulesPath = new File(tempDir, RULES_DIR);
                rulesPath.mkdir();
                //rulesPath.deleteOnExit();
                File sourceClassesPath = new File(tempDir, SOURCE_CLASSES_DIR);
                sourceClassesPath.mkdir();
                //sourceClassesPath.deleteOnExit();
                sourceClassesPath.setReadable(true);
                sourceClassesPath.setWritable(true);
                File compiledClassesPath = new File(tempDir, COMPILED_CLASSES_DIR);
                compiledClassesPath.mkdir();
                //compiledClassesPath.deleteOnExit();
                compiledClassesPath.setReadable(true);
                compiledClassesPath.setWritable(true);
                ctx.setSessionAttribute(UPLOAD_DIR_PARAM, uploadPath);
                ctx.setSessionAttribute(RULES_DIR_PARAM, rulesPath);
                ctx.setSessionAttribute(SOURCE_CLASSES_DIR_PARAM, sourceClassesPath);
                ctx.setSessionAttribute(COMPILED_CLASSES_DIR_PARAM, compiledClassesPath);
                ctx.setSessionAttribute(SESSION_ID_PARAM, sessionId);
            }
            String contentType = StringUtils.defaultIfBlank(request.headers("Content-Type"), "");
            if (contentType.startsWith("multipart/form-data") || contentType.startsWith("application/x-www-form-urlencoded")) {
                request.raw().setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement);
                request.raw().getParts();
            }
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Request-Method", "*");
            response.header("Access-Control-Allow-Headers", "*");
            setCORS(request, response);
            String body = request.body();
            if (StringUtils.isNotBlank(body)) {
                if ("application/json".equals(contentType)) {
                    body = jsonUtils.toJSON(jsonUtils.fromJSON(body, Object.class));
                }
                if (log.isDebugEnabled()) {
                    log.info("Request for : " + request.requestMethod() + " " + request.uri() + "\n" + body);
                } else {
                    log.info("Request for : " + request.requestMethod() + " " + request.uri());
                }
            } else {
                log.info("Request for : " + request.requestMethod() + " " + request.uri());
            }
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

