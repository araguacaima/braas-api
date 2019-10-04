package com.araguacaima.braas.api.routes;

import com.araguacaima.braas.api.BeanBuilder;
import com.araguacaima.braas.api.common.Commons;
import com.araguacaima.braas.api.controller.ApiController;
import com.araguacaima.braas.api.controller.MongoAccess;
import com.araguacaima.braas.api.model.BraasDrools;
import com.araguacaima.braas.core.Constants;
import com.araguacaima.braas.core.drools.DroolsConfig;
import com.araguacaima.braas.core.drools.DroolsUtils;
import com.araguacaima.commons.utils.FileUtils;
import com.araguacaima.commons.utils.StringUtils;
import com.github.victools.jsonschema.generator.Option;
import com.google.common.collect.ImmutableList;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.LocaleUtils;
import org.pac4j.sparkjava.SparkWebContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.RouteGroup;

import javax.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static com.araguacaima.braas.api.Server.engine;
import static com.araguacaima.braas.api.common.Commons.*;
import static java.net.HttpURLConnection.*;
import static spark.Spark.*;

public class Api implements RouteGroup {

    public static final String PATH = "/api";
    public static final String JSON_SCHEMA = "/json-schema";
    public static final String API = "Api";
    private static final String RULES_BASE = "/rules-base";
    private static final String ASSETS = "/assets";
    private static final String ENCODED_RULES = "/encoded-rules";
    private static final String ZIP_COMPRESSED_MIME = "application/x-zip-compressed";
    private static final String JAR_MIME = "application/java-archive";
    private static final String OCTET_STREAM_MIME = "application/octet-stream";
    private static final String ZIP_MIME = "application/zip";
    private static final String TEXT_PLAIN = "text/plain";

    protected static Logger log = LoggerFactory.getLogger(Api.class);

    protected static Collection<Option> with = ImmutableList.of(Option.FLATTENED_ENUMS, Option.SIMPLIFIED_ENUMS);
    protected static Collection<Option> without = ImmutableList.of(Option.DEFINITIONS_FOR_ALL_OBJECTS);

    @Override
    public void addRoutes() {
        //before(Commons.EMPTY_PATH + "*", Commons.genericFilter);
        //before(Commons.EMPTY_PATH + "*", Commons.apiFilter);
        get(Commons.EMPTY_PATH, buildRoute(new BeanBuilder().title(BRAAS + BREADCRUMBS_SEPARATOR + API), "/api"), engine);
        before(JSON_SCHEMA, ApiController::setLocalEnvironment);
        post(JSON_SCHEMA, (request, response) -> {
            final SparkWebContext ctx = new SparkWebContext(request, response);
            ApiController.UploadModel uploadModel = new ApiController.UploadModel(ctx).invoke();
            File uploadDir = uploadModel.getUploadDir();
            String classesPath = null;
            String folderName;
            String destinationDir = null;
            String jsonSchema;
            try {
                try {
                    classesPath = storeFileAndGetPathFromMultipart(request, ZIP_PART_NAME, uploadDir);
                    Part part = request.raw().getPart(ZIP_PART_NAME);
                    String contentType = part.getContentType();
                    if (ZIP_COMPRESSED_MIME.equalsIgnoreCase(contentType) || ZIP_MIME.equalsIgnoreCase(contentType)) {
                        File file = new File(classesPath);
                        String extension = Commons.getFileExtension(file);
                        folderName = file.getName().replace("." + extension, StringUtils.EMPTY);
                        destinationDir = uploadDir.getCanonicalPath() + File.separator + folderName;
                        //noinspection ResultOfMethodCallIgnored
                        File file1 = new File(destinationDir);
                        file1.delete();
                        file1.mkdirs();
                        zipUtils.unZip(file, file1);
                    }
                } catch (Throwable ignored) {
                    classesPath = storeFileAndGetPathFromMultipart(request, JAR_PART_NAME, uploadDir);
                    Part part = request.raw().getPart(JAR_PART_NAME);
                    String contentType = part.getContentType();
                    if (JAR_MIME.equalsIgnoreCase(contentType) || OCTET_STREAM_MIME.equalsIgnoreCase(contentType)) {
                        File file = new File(classesPath);
                        String extension = Commons.getFileExtension(file);
                        folderName = file.getName().replace("." + extension, StringUtils.EMPTY);
                        destinationDir = uploadDir.getCanonicalPath() + File.separator + folderName;
                        //noinspection ResultOfMethodCallIgnored
                        File file1 = new File(destinationDir);
                        file1.delete();
                        file1.mkdirs();
                        jarUtils.unZip(destinationDir, file1.getCanonicalPath());
                    }
                }
                if (StringUtils.isNotBlank(destinationDir)) {
                    classesPath = destinationDir;
                    log.debug("Classes path '" + classesPath + "' loaded!");
                    File directory = new File(classesPath);
                    Iterator<File> files = FileUtils.iterateFilesAndDirs(directory, new SuffixFileFilter(CLASS_SUFFIX), TrueFileFilter.INSTANCE);
                    Map<String, String> jsonSchemas = new HashMap<>();
                    while (files.hasNext()) {
                        File file = files.next();
                        jsonUtils.buildJsonSchemaMapFromClassFile(directory, file, jsonSchemas, with, without);
                    }
                    jsonSchema = "[" + String.join(",", jsonSchemas.values()) + "]";
                    response.status(HTTP_CREATED);
                    return jsonSchema;
                }
                response.status(HTTP_CONFLICT);
            } catch (Throwable ex) {
                ex.printStackTrace();
                try {
                    if (classesPath != null) {
                        //noinspection ResultOfMethodCallIgnored
                        new File(classesPath).delete();
                    }
                    if (destinationDir != null) {
                        //noinspection ResultOfMethodCallIgnored
                        new File(destinationDir).delete();
                    }
                } catch (Throwable ignored) {
                }
                response.status(HTTP_INTERNAL_ERROR);
            }
            return EMPTY_RESPONSE;
        });
        before(Api.RULES_BASE + BRAAS_SESSION_ID_PATH_PARAM + ASSETS, ApiController::setNamespace);
        post(Api.RULES_BASE + BRAAS_SESSION_ID_PATH_PARAM + ASSETS, (request, response) -> {
            final SparkWebContext ctx = new SparkWebContext(request, response);
            Locale locale = null;
            String localeStr = request.queryParams("locale");
            try {
                if (StringUtils.isNotBlank(localeStr)) {
                    locale = LocaleUtils.toLocale(localeStr);
                }
            } catch (IllegalArgumentException ignored) {

            }
            String rulesTabName = request.queryParams("rules-tab-name");
            try {
                if (StringUtils.isBlank(rulesTabName)) {
                    rulesTabName = DroolsUtils.RULES_TABLES_DEFAULT_NAME;
                }
            } catch (IllegalArgumentException ignored) {

            }
            String braasSessionId = request.params(BRAAS_SESSION_ID_PARAM);

            BraasDrools braasDrools = MongoAccess.getBraasDroolsById(BRAAS_DROOLS_PARAM, braasSessionId);
            if (braasDrools == null) {
                return Commons.throwError(response, 424, new Exception("Rule base spreadsheet is not present on request not previously provided. Make sure you provide it according to API specification [http://braaservice.com/api#/Rules_base]"));
            } else {
                if (braasDrools.getSpreadsheet() == null || braasDrools.getSpreadsheet().getBinary() == null) {
                    return Commons.throwError(response, 424, new Exception("Rule base spreadsheet is not present on request not previously provided. Make sure you provide it according to API specification [http://braaservice.com/api#/Rules_base]"));
                }
            }

            DroolsConfig droolsConfig = (DroolsConfig) ctx.getSessionAttribute(DROOLS_CONFIG_PARAM);
            if (droolsConfig == null) {
                try {
                    ApiController.RuleBaseModel ruleBaseModel = new ApiController.RuleBaseModel(ctx).invoke();
                    File sourceClassesDir = ruleBaseModel.getSourceClassesDir();
                    File compiledClassesDir = ruleBaseModel.getCompiledClassesDir();
                    URLClassLoader classLoader = ApiController.buildClassesFromSchema(braasDrools, sourceClassesDir, compiledClassesDir);
                    if (classLoader != null) {
                        droolsConfig = ApiController.createDroolsConfig(
                                braasDrools.getSpreadsheet().getBinary(), classLoader, droolsConfig, Constants.URL_RESOURCE_STRATEGIES.ABSOLUTE_DECISION_TABLE_PATH);
                        ctx.setSessionAttribute(DROOLS_CONFIG_PARAM, droolsConfig);
                    } else {
                        return Commons.throwError(response, HTTP_INTERNAL_ERROR, new Exception("It was not possible to load your provided schema to be used later in your rule's base"));
                    }
                } catch (Throwable t) {
                    return Commons.throwError(response, 424, new Exception("Rule base spreadsheet is not present on request not previously provided. Make sure you provide it according to API specification [http://braaservice.com/api#/Rules_base]", t));
                }
            }
            droolsConfig.setLocale(locale);
            droolsConfig.setRulesTabName(rulesTabName);

            Collection<?> results = ApiController.processAssets(droolsConfig, request);

            if (results != null) {
                response.status(HTTP_ACCEPTED);
                response.type(JSON_CONTENT_TYPE);
                return jsonUtils.toJSON(results);
            }
            return Commons.throwError(response, HTTP_INTERNAL_ERROR, new Exception("It was not possible to load your provided schema to be used later in your rule's base"));
        });
        post(ENCODED_RULES, (request, response) -> {
            try {
                final SparkWebContext ctx = new SparkWebContext(request, response);
                ApiController.UploadModel uploadModel = new ApiController.UploadModel(ctx).invoke();
                File uploadDir = uploadModel.getUploadDir();
                String rulesPath;
                try {
                    rulesPath = ApiController.extractSpreadSheet(request, uploadDir);
                    String encoded = Commons.encodeFileToBase64(rulesPath);
                    response.status(HTTP_OK);
                    response.type(TEXT_PLAIN);
                    return encoded;
                } catch (Throwable t) {
                    return Commons.throwError(response, HTTP_CONFLICT, new Exception("Rule base spreadsheet is not present on request. Make sure you provide it according to API specification [http://braaservice.com/api#/Rules_base/add_base]", t));
                }
            } catch (Throwable ex) {
                ex.printStackTrace();
                response.status(HTTP_INTERNAL_ERROR);
            }
            return EMPTY_RESPONSE;
        });
    }

    public static class ApiFile extends Api implements RouteGroup {
        public static final String PATH = Api.PATH + Api.RULES_BASE + BRAAS_SESSION_ID_PATH_PARAM + "/file";

        @Override
        public void addRoutes() {
            //before(Commons.EMPTY_PATH, Commons.genericFilter);
            //before(Commons.EMPTY_PATH, Commons.apiFilter);
            before(Commons.EMPTY_PATH, ApiController::setNamespace);
            put(Commons.EMPTY_PATH, (request, response) -> {
                final SparkWebContext ctx = new SparkWebContext(request, response);
                try {
                    String braasSessionId = request.params(BRAAS_SESSION_ID_PARAM);
                    BraasDrools braasDrools = (BraasDrools) ctx.getSessionAttribute(BRAAS_DROOLS_PARAM);
                    if (braasDrools == null || !braasDrools.getBraasId().equals(braasSessionId)) {
                        braasDrools = ApiController.buildBraasDrools(braasSessionId, ctx);
                        if (braasDrools == null) {
                            ctx.setSessionAttribute(BRAAS_DROOLS_PARAM, null);
                            ctx.setSessionAttribute(DROOLS_CONFIG_PARAM, null);
                            return Commons.throwError(response, 424, new Exception("Rule base spreadsheet is not previously provided. Make sure you provide it according to API specification [http://braaservice.com/api#/Rules_base]"));
                        }
                    }
                    ApiController.RuleBaseModel ruleBaseModel = new ApiController.RuleBaseModel(ctx).invoke();
                    File rulesDir = ruleBaseModel.getRulesDir();
                    File sourceClassesDir = ruleBaseModel.getSourceClassesDir();
                    File compiledClassesDir = ruleBaseModel.getCompiledClassesDir();
                    String rulesPath;
                    try {
                        rulesPath = ApiController.extractSpreadSheet(request, rulesDir);
                    } catch (Throwable t) {
                        ctx.setSessionAttribute(BRAAS_DROOLS_PARAM, null);
                        ctx.setSessionAttribute(DROOLS_CONFIG_PARAM, null);
                        return Commons.throwError(response, HTTP_CONFLICT, new Exception("Rule base spreadsheet is not present on request. Make sure you provide it according to API specification [http://braaservice.com/api#/Rules_base/add_base]", t));
                    }
                    String binary = encodeFileToBase64(rulesPath);
                    String schemaPath;
                    try {
                        schemaPath = ApiController.extractSchema(request, rulesDir);
                    } catch (Throwable t) {
                        ctx.setSessionAttribute(BRAAS_DROOLS_PARAM, null);
                        ctx.setSessionAttribute(DROOLS_CONFIG_PARAM, null);
                        return Commons.throwError(response, HTTP_CONFLICT, new Exception("Json schema is not present on request. Make sure you provide it according to API specification [http://braaservice.com/api#/Rules_base/add_base]", t));
                    }
                    File schemaFile = new File(schemaPath);
                    URLClassLoader classLoader = ApiController.buildClassesFromSchema(
                            schemaFile, getFileNameFromPart(request.raw().getPart(FILE_NAME_PREFIX)), sourceClassesDir, compiledClassesDir);
                    if (classLoader != null) {
                        DroolsConfig droolsConfig = ApiController.createDroolsConfig(binary, classLoader, null, Constants.URL_RESOURCE_STRATEGIES.ABSOLUTE_DECISION_TABLE_PATH);
                        DroolsUtils droolsUtils = new DroolsUtils(droolsConfig);
                        try {
                            droolsUtils.executeRules(new Object());
                        } catch (Throwable t) {
                            ctx.setSessionAttribute(BRAAS_DROOLS_PARAM, null);
                            ctx.setSessionAttribute(DROOLS_CONFIG_PARAM, null);
                            return Commons.throwError(response, HTTP_INTERNAL_ERROR, new Exception("It was not possible to load your provided schema to be used later in your rule's base", t));
                        }
                        ctx.setSessionAttribute(DROOLS_CONFIG_PARAM, droolsConfig);
                        String schemas_ = FileUtils.readFileToString(schemaFile, StandardCharsets.UTF_8);
                        braasDrools.setSchemas(schemas_);
                        BraasDrools.Spreadsheet spreadsheet_ = new BraasDrools.Spreadsheet();
                        spreadsheet_.setBinary(binary);
                        braasDrools.setSpreadsheet(spreadsheet_);
                        BraasDrools braasDrools_ = MongoAccess.updateBraasDrools(BRAAS_DROOLS_PARAM, braasDrools);
                        if (braasDrools_ != null) {
                            ctx.setSessionAttribute(DROOLS_CONFIG_PARAM, null);
                            ctx.setSessionAttribute(BRAAS_DROOLS_PARAM, braasDrools_);
                            response.status(HTTP_CREATED);
                        } else {
                            ctx.setSessionAttribute(BRAAS_DROOLS_PARAM, null);
                            ctx.setSessionAttribute(DROOLS_CONFIG_PARAM, null);
                            return Commons.throwError(response, HTTP_INTERNAL_ERROR, new Exception("It was not possible to load your provided schema to be used later in your rule's base"));
                        }
                    } else {
                        ctx.setSessionAttribute(BRAAS_DROOLS_PARAM, null);
                        ctx.setSessionAttribute(DROOLS_CONFIG_PARAM, null);
                        return Commons.throwError(response, HTTP_INTERNAL_ERROR, new Exception("It was not possible to load your provided schema to be used later in your rule's base"));
                    }
                } catch (Throwable ex) {
                    ctx.setSessionAttribute(BRAAS_DROOLS_PARAM, null);
                    ctx.setSessionAttribute(DROOLS_CONFIG_PARAM, null);
                    ex.printStackTrace();
                    response.status(HTTP_INTERNAL_ERROR);
                }
                return EMPTY_RESPONSE;
            });
            get(Commons.EMPTY_PATH, (request, response) -> {
                final SparkWebContext ctx = new SparkWebContext(request, response);
                String fileName = (String) ctx.getSessionAttribute(RULES_BASE_FILE_NAME_PARAM);
                try {
                    Path filePath = Paths.get("temp").resolve(fileName);
                    File file = filePath.toFile();
                    if (file.exists()) {
                        try {
                            response.status(HTTP_OK);
                            return String.join("", Files.readAllLines(filePath));
                        } catch (IOException e) {
                            response.status(HTTP_INTERNAL_ERROR);
                            return "Exception occurred while reading file" + e.getMessage();
                        }
                    } else {
                        response.status(HTTP_INTERNAL_ERROR);
                        return "Rule does not exists";
                    }
                } catch (Throwable ex) {
                    ex.printStackTrace();
                    response.status(HTTP_INTERNAL_ERROR);
                }
                response.raw();
                return EMPTY_RESPONSE;
            });
        }
    }

    public static class ApiGoogleDrive extends Api implements RouteGroup {
        public static final String PATH = Api.PATH + Api.RULES_BASE + BRAAS_SESSION_ID_PATH_PARAM + "/google-drive";

        @Override
        public void addRoutes() {
            /*
            //before(Commons.EMPTY_PATH, Commons.genericFilter);
            //before(Commons.EMPTY_PATH, Commons.apiFilter);
            */
            before(Commons.EMPTY_PATH, ApiController::setNamespace);
        }
    }

    public static class ApiBinary extends Api implements RouteGroup {
        public static final String PATH = Api.PATH + Api.RULES_BASE + BRAAS_SESSION_ID_PATH_PARAM + "/binary";

        @Override
        public void addRoutes() {
            /*
            //before(Commons.EMPTY_PATH, Commons.genericFilter);
            //before(Commons.EMPTY_PATH, Commons.apiFilter);
            */
            before(Commons.EMPTY_PATH, ApiController::setNamespace);
            put(Commons.EMPTY_PATH, (request, response) -> {
                try {
                    final SparkWebContext ctx = new SparkWebContext(request, response);
                    ApiController.RuleBaseModel ruleBaseModel = new ApiController.RuleBaseModel(ctx).invoke();
                    File sourceClassesDir = ruleBaseModel.getSourceClassesDir();
                    File compiledClassesDir = ruleBaseModel.getCompiledClassesDir();
                    String braasSessionId = (String) ctx.getSessionAttribute(BRAAS_SESSION_ID_PARAM);
                    BraasDrools braasDrools;
                    try {
                        braasDrools = ApiController.extractBinary(request);
                        braasDrools.setBraasId(braasSessionId);
                        braasDrools = MongoAccess.updateBraasDrools(BRAAS_DROOLS_PARAM, braasDrools);
                    } catch (Throwable t) {
                        return Commons.throwError(response, HTTP_CONFLICT, new Exception("Incoming body doesn't match with required object structure. Make sure you provide it according to API specification [http://braaservice.com/api#/Rules_base/add-or-replace-binary-rules-base]", t));
                    }
                    URLClassLoader classLoader = ApiController.buildClassesFromSchema(braasDrools, sourceClassesDir, compiledClassesDir);
                    if (classLoader != null) {
                        response.status(HTTP_CREATED);
                    } else {
                        return Commons.throwError(response, HTTP_INTERNAL_ERROR, new Exception("It was not possible to load your provided schema to be used later in your rule's base"));
                    }
                } catch (Throwable ex) {
                    ex.printStackTrace();
                    response.status(HTTP_INTERNAL_ERROR);
                }
                return EMPTY_RESPONSE;
            });
            get(Commons.EMPTY_PATH, (request, response) -> {
                final SparkWebContext ctx = new SparkWebContext(request, response);
                String fileName = (String) ctx.getSessionAttribute(RULES_BASE_FILE_NAME_PARAM);
                try {
                    Path filePath = Paths.get("temp").resolve(fileName);
                    File file = filePath.toFile();
                    if (file.exists()) {
                        try {
                            response.status(HTTP_OK);
                            return String.join("", Files.readAllLines(filePath));
                        } catch (IOException e) {
                            response.status(HTTP_INTERNAL_ERROR);
                            return "Exception occurred while reading file" + e.getMessage();
                        }
                    } else {
                        response.status(HTTP_INTERNAL_ERROR);
                        return "Rule does not exists";
                    }
                } catch (Throwable ex) {
                    ex.printStackTrace();
                    response.status(HTTP_INTERNAL_ERROR);
                }
                response.raw();
                return EMPTY_RESPONSE;
            });
        }
    }

}
