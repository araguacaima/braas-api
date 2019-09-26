package com.araguacaima.braas.api.common;

import com.araguacaima.braas.api.BeanBuilder;
import com.araguacaima.braas.api.ConfigFactory;
import com.araguacaima.braas.api.MessagesWrapper;
import com.araguacaima.braas.api.Server;
import com.araguacaima.braas.api.filter.SessionFilter;
import com.araguacaima.braas.api.model.BraasDrools;
import com.araguacaima.braas.api.wrapper.AccountWrapper;
import com.araguacaima.braas.api.wrapper.RolesWrapper;
import com.araguacaima.braas.core.drools.DroolsConfig;
import com.araguacaima.braas.core.drools.DroolsUtils;
import com.araguacaima.braas.core.drools.OSValidator;
import com.araguacaima.braas.core.google.model.Account;
import com.araguacaima.braas.core.google.model.Role;
import com.araguacaima.commons.utils.FileUtils;
import com.araguacaima.commons.utils.JsonUtils;
import com.araguacaima.commons.utils.ReflectionUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.beanutils.BeanMap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.pac4j.core.config.Config;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.sparkjava.SparkWebContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.*;
import spark.route.HttpMethod;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

import static com.araguacaima.braas.api.Server.*;
import static com.araguacaima.braas.api.common.Security.JWT_SALT;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_OK;

public class Commons {

    public static final String DELETE_MODEL_ROLE = "delete:model";
    public static final String WRITE_MODEL_ROLE = "write:model";
    public static final String READ_MODELS_ROLE = "read:models";
    public static final String DELETE_CATALOG_ROLE = "delete:catalog";
    public static final String WRITE_CATALOG_ROLE = "write:catalog";
    public static final String READ_CATALOGS_ROLE = "read:catalogs";
    public static final String DELETE_PALETTE_ROLE = "delete:palette";
    public static final String WRITE_PALETTE_ROLE = "write:palette";
    public static final String READ_PALETTES_ROLE = "read:palettes";
    public static final String ADMIN_ROLE = "admin";
    public static final List<String> ALL_ROLES = Arrays.asList(
            DELETE_MODEL_ROLE, WRITE_MODEL_ROLE, READ_MODELS_ROLE,
            DELETE_CATALOG_ROLE, WRITE_CATALOG_ROLE, READ_CATALOGS_ROLE,
            DELETE_PALETTE_ROLE, WRITE_PALETTE_ROLE, READ_PALETTES_ROLE, ADMIN_ROLE);
    public static final String clients = "Google2Client";
    public static final String JSON_CONTENT_TYPE = "application/json";
    public static final String HTML_CONTENT_TYPE = "text/html";
    public static final String EMPTY_RESPONSE = StringUtils.EMPTY;
    public static final String BRAAS = "BraaS";
    public static final String PRODUCT = "Bussines Rules as a Service";
    public static final String PRODUCT_DESCRIPTION = "Out-of-the-box rules execution thru API";
    public static final JsonUtils jsonUtils = new JsonUtils();
    public static final String DEFAULT_PATH = "/";
    public static final String EMPTY_PATH = "";
    public static final String SEPARATOR_PATH = "/";
    public static final String REJECTED_SCOPES = "rejected_scopes";

    public static final String TEMP_DIR = "tmp";
    public static final String TEMP_DIR_PARAM = "tempDir";
    public static final String UPLOAD_DIR = "upload";
    public static final String RULES_DIR = "rules";
    public static final String SOURCE_CLASSES_DIR = "source-classes";
    public static final String COMPILED_CLASSES_DIR = "compiled-classes";
    public static final String BRAAS_SESSION_ID_PARAM = "braasSessionId";
    public static final String BRAAS_SESSION_ID_PATH_PARAM = "/:" + BRAAS_SESSION_ID_PARAM;
    public static final String BRAAS_DROOLS_PARAM = "braasDrools";
    public static final String DROOLS_CONFIG_PARAM = "drools-config";
    public static final String UPLOAD_DIR_PARAM = "uploadDir";
    public static final String RULES_DIR_PARAM = "rulesDir";
    public static final String SOURCE_CLASSES_DIR_PARAM = "sourceClassesDir";
    public static final String COMPILED_CLASSES_DIR_PARAM = "compiledClassesDir";
    public static final String RULES_BASE_FILE_NAME_PARAM = "rules-base-file-name";
    public static final String BREADCRUMBS_SEPARATOR = " | ";
    public static final String FILE_NAME_PREFIX = "spreadsheet";
    public static final String ZIP_PART_NAME = "zip";
    public static final String JAR_PART_NAME = "jar";
    public static final String CLASS_SUFFIX = "class";
    public static final String JSON_SUFFIX = "json";
    public static final String JSON_SCHEMA_FILE_NAME = "braas-json-schemas.json";
    public static final String SPREADSHEET_FILE_EXTENSION = ".xlsx";
    public static final ReflectionUtils reflectionUtils = new ReflectionUtils(null);
    public static final String BRAAS_RULES_FILE_NAME = "braas-rules.xlsx";
    final static Config config = new ConfigFactory(JWT_SALT, engine).build(deployedServer, DEFAULT_PATH, clients);
    private static Logger log = LoggerFactory.getLogger(Commons.class);
    public static final ExceptionHandler exceptionHandler = new ExceptionHandlerImpl(Exception.class) {
        @Override
        public void handle(Exception exception, Request request, Response response) {
            String message = exception.getMessage();
            response.type("text/html");
            StackTraceElement[] stackTrace = exception.getStackTrace();
            if (message == null) {
                try {
                    message = exception.getCause().getMessage();
                } catch (Throwable ignored) {
                }
                if (message == null) {
                    List<StackTraceElement> ts = Arrays.asList(stackTrace);
                    message = StringUtils.join(ts);
                }
            }
            log.error("Error '" + message + "'");
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("title", "Error");
            errorMap.put("message", message);
            errorMap.put("stack", stackTrace);
            try {
                response.body(jsonUtils.toJSON(errorMap));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };
    //final static CallbackRoute callback = new CallbackRoute(config, null, true);
    //static Filter strongSecurityFilter = Authentication.buildStrongSecurityFilter(config);
    //static Filter adminApiFilter = new AdminAPIFilter(config, clients, "adminAuthorizer,custom," + DefaultAuthorizers.ALLOW_AJAX_REQUESTS + "," + DefaultAuthorizers.IS_AUTHENTICATED);
    //static Filter apiFilter = new APIFilter(config, clients, "checkHttpMethodAuthorizer,requireAnyRoleAuthorizer,custom," + DefaultAuthorizers.ALLOW_AJAX_REQUESTS + "," + DefaultAuthorizers.IS_AUTHENTICATED);
    //static Filter scopesFilter = new ScopesFilter(config, clients, "filterAllRolesAuthorizer");

    static {
        ObjectMapper mapper = jsonUtils.getMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public static String render(Map<String, Object> model, String templatePath) {
        return engine.render(buildModelAndView(model, templatePath));
    }

    public static TemplateViewRoute buildRoute(String path) {
        return (request, response) -> buildModelAndView(request, response, null, path);
    }

    public static TemplateViewRoute buildRoute(Object bean, String path) {
        return (request, response) -> buildModelAndView(request, response, bean, path);
    }

    public static ModelAndView buildModelAndView(Object bean, String path) {
        return buildModelAndView(null, null, bean, path);
    }

    public static ModelAndView buildModelAndView(Request request, Response response, Object bean, String path) {
        Map<Object, Object> map = new HashMap<>();
        if (bean != null) {
            if (Map.class.isAssignableFrom(bean.getClass())) {
                map.putAll((Map<Object, Object>) bean);
            } else {
                if (BeanBuilder.class.isAssignableFrom(bean.getClass())) {
                    ((BeanBuilder) bean).fixAccountInfo(request, response);
                }
                map.putAll(new BeanMap(bean));
                map.remove("class");
            }
        }
        final Map<String, Object> newMap = new HashMap<>();
        map.forEach((o, o2) -> {
            String key = o.toString();
            try {
                if (o2 != null) {
                    Class<?> clazz = o2.getClass();
                    if (ReflectionUtils.isCollectionImplementation(clazz)) {
                        if (Collection.class.isAssignableFrom(clazz)) {
                            newMap.put(key, o2);
                            newMap.put(key + "_", jsonUtils.toJSON(o2, true));

                        } else if (Object[].class.isAssignableFrom(clazz)
                                || clazz.isArray()) {
                            newMap.put(key, o2);
                            newMap.put(key + "_", jsonUtils.toJSON(o2, true));
                        }
                    } else {
                        if (reflectionUtils.getFullyQualifiedJavaTypeOrNull(clazz) == null) {
                            newMap.put(key, o2);
                            newMap.put(key + "_", jsonUtils.toJSON(o2, true));
                        } else {
                            newMap.put(key, o2);
                        }
                    }
                } else {
                    newMap.put(key, null);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        while (newMap.values().remove(null)) ;
        return new ModelAndView(newMap, path);
    }

    public static Map<HttpMethod, Map<InputOutput, Object>> setOptionsOutputStructure(
            Object object_httpMethod_1, Object object_httpMethod_2, HttpMethod httpMethod_1, HttpMethod httpMethod_2) {
        Map<HttpMethod, Map<InputOutput, Object>> output = new HashMap<>();
        if (object_httpMethod_1 != null) {
            Map<InputOutput, Object> output_httpMethod_1 = new HashMap<>();
            output_httpMethod_1.put(InputOutput.output, object_httpMethod_1);
            output.put(httpMethod_1, output_httpMethod_1);
        }
        if (object_httpMethod_2 != null) {
            Map<InputOutput, Object> input_httpMethod_2 = new HashMap<>();
            input_httpMethod_2.put(InputOutput.input, object_httpMethod_2);
            output.put(httpMethod_2, input_httpMethod_2);
        }
        return output;
    }

    public static String getOptions(Request request, Response response, Map<HttpMethod, Map<InputOutput, Object>> object) throws IOException {
        response.status(HTTP_OK);
        response.header("Allow", "GET");
        Map<String, Object> jsonMap = new HashMap<>();

        String contentType = getContentType(request);
        response.header("Content-Type", contentType);
        try {
            if (contentType.equals(HTML_CONTENT_TYPE)) {
                String json = request.pathInfo().replaceFirst(request.pathInfo(), "");
                jsonMap.put("title", StringUtils.capitalize(json));
                jsonMap.put("json", jsonUtils.toJSON(object));
                return render(jsonMap, "json");
            } else {
                return jsonUtils.toJSON(object);
            }
        } catch (Throwable t) {
            jsonMap = new HashMap<>();
            jsonMap.put("error", t.getMessage());
            if (contentType.equals(HTML_CONTENT_TYPE)) {
                return render(jsonMap, "json");
            } else {
                return jsonUtils.toJSON(jsonMap);
            }
        }
    }

    public static String throwError(Response response, Throwable ex) {
        return throwError(response, HTTP_INTERNAL_ERROR, ex);
    }

    public static String throwError(Response response, int responseStatus, Throwable ex) {
        response.status(responseStatus);
        response.type(JSON_CONTENT_TYPE);
        try {
            return jsonUtils.toJSON(MessagesWrapper.fromExceptionToMessages(ex, HTTP_INTERNAL_ERROR));
        } catch (IOException e) {
            return ex.getMessage();
        }
    }

    public static String getContentType(Request request) {
        String accept = request.headers("Accept");
        if (accept == null) {
            accept = request.headers("ACCEPT");
        }
        if (accept == null) {
            accept = request.headers("accept");
        }
        if (accept != null && accept.trim().equalsIgnoreCase(HTML_CONTENT_TYPE)) {
            return HTML_CONTENT_TYPE;
        } else {
            return JSON_CONTENT_TYPE;
        }
    }

    public static String renderContent(String htmlFile) {
        try {
            // If you are using maven then your files
            // will be in a folder called resources.
            // getResource() gets that folder
            // and any files you specify.
            URL url = Server.class.getResource("/web/" + htmlFile);

            // Return a String which has all
            // the contents of the file.
            Path path = Paths.get(url.toURI());
            return new String(Files.readAllBytes(path), Charset.defaultCharset());
        } catch (IOException | URISyntaxException e) {
            // Add your own exception handlers here.
        }
        return null;
    }

    public static String yamlToJsonString(String yaml) throws IOException {
        ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
        Object obj = yamlReader.readValue(yaml, Object.class);
        return jsonUtils.toJSON(obj, true);
    }

    public static Map yamlToJsonMap(String yaml) throws IOException {
        ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
        return yamlReader.readValue(yaml, Map.class);
    }

    public static String getBraasYamlDefinition() {
        try {
            // If you are using maven then your files
            // will be in a folder called resources.
            // getResource() gets that folder
            // and any files you specify.
            URL url = Server.class.getResource("/web/public/braas-apis.yaml");

            // Return a String which has all
            // the contents of the file.
            Path path = Paths.get(url.toURI());
            return new String(Files.readAllBytes(path), Charset.defaultCharset());
        } catch (IOException | URISyntaxException e) {
            // Add your own exception handlers here.
        }
        return null;
    }

    public static Map<String, Object> getBraasDefinition(String definitionName) throws IOException {
        String yaml = getBraasYamlDefinition();
        if (StringUtils.isNotBlank(yaml)) {
            Map jsonMap = yamlToJsonMap(yaml);
            CaseInsensitiveMap definitions = new CaseInsensitiveMap((Map) jsonMap.get("definitions"));
            if (StringUtils.isNotBlank(definitionName)) {
                Map<String, Object> requestedDefinition = (Map) definitions.get(definitionName);
                Map<String, Object> map = new HashMap<>();
                //map.put("$id", "https://example.com/person.schema.json");
                map.put("$schema", "http://json-schema.org/draft-06/schema#");
                map.put("title", definitionName);
                map.putAll(requestedDefinition);
                map.put("definitions", definitions);
                return map;
            } else {
                Map<String, Object> map = new HashMap<>();
                //map.put("$id", "https://example.com/person.schema.json");
                map.put("$schema", "http://json-schema.org/draft-06/schema#");
                map.put("definitions", definitions);
                return map;
            }
        }
        return null;
    }

    public static List<CommonProfile> getProfiles(SparkWebContext context) {
        final ProfileManager<CommonProfile> manager = new ProfileManager<>(context);
        return manager.getAll(true);
    }

    public static List<CommonProfile> getProfiles(final Request request, final Response response) {
        final SparkWebContext context = new SparkWebContext(request, response);
        final ProfileManager<CommonProfile> manager = new ProfileManager<>(context);
        return manager.getAll(true);
    }

    public static CommonProfile findAndFulfillProfile(SparkWebContext context) {
        CommonProfile profile = IterableUtils.find(getProfiles(context), object -> clients.contains(object.getClientName()));
        if (profile != null) {
            Account account = (Account) context.getSessionAttribute("account");
            if (account != null) {
                account.getRoles().forEach(role -> profile.addRole(role.getName()));
            } else {
                String email = profile.getEmail();
                Map<String, Object> params = new HashMap<>();
            }
        }
        return profile;
    }

    public static CommonProfile findProfile(SparkWebContext context) {
        return findProfile(getProfiles(context));
    }

    public static CommonProfile findProfile(Request req, Response res) {
        return findProfile(new SparkWebContext(req, res));
    }

    public static CommonProfile findProfile(List<CommonProfile> profiles) {
        return IterableUtils.find(profiles, object -> clients.contains(object.getClientName()));
    }

    public static void store(Request req, Response res) {
        SparkWebContext context = new SparkWebContext(req, res);
        CommonProfile profile = findAndFulfillProfile(context);
        if (profile != null) {
            Account account = null;
            String email = profile.getEmail();
            Map<String, Object> params = new HashMap<>();
            params.put(Account.PARAM_EMAIL, email);

            if (account == null) {
                account = AccountWrapper.toAccount(profile);
            }
            SessionFilter.SessionMap map = SessionFilter.map.get(email);
            if (map == null) {
                map = new SessionFilter.SessionMap(req.session(), true);
                SessionFilter.map.put(email, map);
            }
            if (account.isEnabled()) {

                Set<Role> accountRoles = account.getRoles();
                final Set<Role> roles = accountRoles;

                Set<String> profileRoles = profile.getRoles();
                if (CollectionUtils.isNotEmpty(profileRoles)) {
                    profileRoles.forEach(role -> {
                        Map<String, Object> roleParams = new HashMap<>();
                        roleParams.put(Role.PARAM_NAME, role);
                        Role role_ = null;
                        if (role_ == null) {
                            Role newRole = RolesWrapper.buildRole(role);
                            roles.add(newRole);
                        }
                    });
                } else {
                    Commons.ALL_ROLES.forEach(role -> {
                        fixRole(accountRoles, roles, role);
                    });
                }

                profile.addRoles(RolesWrapper.fromRoles(accountRoles));

                context.setSessionAttribute("account", account);
            }
        }
    }

    private static void fixRole(Set<Role> accountRoles, Set<Role> roles, String roleName) {
        Map<String, Object> roleParams = new HashMap<>();
        Role roleWriteCatalog = RolesWrapper.buildRole(roleName);
        roleParams.put(Role.PARAM_NAME, roleWriteCatalog.getName());
        Role role_ = null;
        if (role_ == null) {
            roles.add(roleWriteCatalog);
        } else {
            Role innerRole = IterableUtils.find(accountRoles, role -> role.getName().equals(roleWriteCatalog.getName()));
            if (innerRole == null) {
                roles.add(role_);
            }
        }
    }

    public static Collection<Object> validate(Map model, Map<String, String> rulesConfig) throws Exception {
        if (MapUtils.isNotEmpty(rulesConfig)) {
            Properties properties = MapUtils.toProperties(rulesConfig);
            DroolsConfig droolsConfig = new DroolsConfig(properties);
            InputStream credentialsStream = new StringBufferInputStream(rulesConfig.get("credentials"));
            droolsConfig.setCredentialsStream(credentialsStream);
            String rulesPath = rulesConfig.get("rulesPath");
            String rulesRepositoryStrategy = rulesConfig.get("rulesRepositoryStrategy");
            String urlResourceStrategy = rulesConfig.get("urlResourceStrategy");
            if (org.apache.commons.lang.StringUtils.isNotBlank(rulesPath) && org.apache.commons.lang.StringUtils.isNotBlank(rulesRepositoryStrategy) && org.apache.commons.lang.StringUtils.isNotBlank(urlResourceStrategy)) {
                droolsConfig.setRulesPath(rulesPath);
                droolsConfig.setRulesRepositoryStrategy(rulesRepositoryStrategy);
                droolsConfig.setUrlResourceStrategy(urlResourceStrategy);
            }
            DroolsUtils droolsUtils = new DroolsUtils(droolsConfig);
            return droolsUtils.executeRules(model);

        }
        return new ArrayList<>();
    }

    public static String getStringFromMultipart(Request request, String partName) throws IOException, ServletException {
        HttpServletRequest raw = request.raw();
        raw.setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement);
        Part part = raw.getPart(partName);
        return IOUtils.toString(part.getInputStream(), StandardCharsets.UTF_8);
    }

    public static String storeFileAndGetPathFromMultipart(Request request, String partName, File directory, String fileName) throws IOException, ServletException {
        HttpServletRequest raw = request.raw();
        raw.setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement);
        Part uploadedFile = raw.getPart(partName);
        Path out = Paths.get(directory.getCanonicalPath() + "/" + fileName);
        try (final InputStream in = uploadedFile.getInputStream()) {
            Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
        }
        uploadedFile = null;
        return out.toFile().getCanonicalPath();
    }

    public static String storeFileAndGetPathFromBinary(BraasDrools braasDroolsSpreadsheet, File directory, String fileName) throws IOException {
        File file = new File(directory, fileName);
        FileUtils.writeByteArrayToFile(file, braasDroolsSpreadsheet.getBinary_());
        return file.getCanonicalPath();
    }

    public static String getFilePath(String fileName) throws URISyntaxException, NullPointerException {
        URL resource = Server.class.getClassLoader().getResource("./" + fileName);
        String path = resource.toURI().getPath();
        if (OSValidator.isWindows() && path.startsWith("/")) {
            path = path.substring(1);
        }
        return path;
    }

    public static String getFileNameFromPart(Part part) {
        for (String cd : part.getHeader("content-disposition").split(";")) {
            if (cd.trim().startsWith("filename")) {
                return cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
            }
        }
        return null;
    }

    public static String encodeFileToBase64(String fileName) throws IOException {
        return encodeFileToBase64(new File(fileName));
    }

    public static String encodeFileToBase64(File file) throws IOException {
        byte[] result = FileUtils.readFileToByteArray(file);
        return encodeFileToBase64(result);
    }

    public static String encodeFileToBase64(byte[] result) {
        return Base64.getEncoder().encodeToString(result);
    }

    public enum InputOutput {
        input,
        output
    }

}
