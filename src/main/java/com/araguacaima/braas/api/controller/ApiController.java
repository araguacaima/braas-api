package com.araguacaima.braas.api.controller;

import com.araguacaima.braas.api.exception.InternalBraaSException;
import com.araguacaima.braas.api.model.Binary;
import com.araguacaima.braas.core.Constants;
import com.araguacaima.braas.core.drools.DroolsConfig;
import com.araguacaima.braas.core.drools.DroolsURLClassLoader;
import com.araguacaima.braas.core.drools.DroolsUtils;
import com.araguacaima.commons.exception.core.PropertiesUtilException;
import com.araguacaima.commons.utils.FileUtils;
import com.araguacaima.commons.utils.JsonSchemaUtils;
import com.araguacaima.commons.utils.PropertiesHandler;
import com.araguacaima.commons.utils.StringUtils;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.kie.api.KieBase;
import org.pac4j.sparkjava.SparkWebContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.utils.CollectionUtils;

import javax.servlet.ServletException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.araguacaima.braas.api.Server.multipartConfigElement;
import static com.araguacaima.braas.api.common.Commons.*;
import static com.araguacaima.braas.core.drools.utils.RuleMessageUtils.getMessages;

public class ApiController {

    private static Logger log = LoggerFactory.getLogger(ApiController.class);

    public static URLClassLoader buildClassesFromSchema(File schemaFile, File sourceClassesDir, File compiledClassesDir) throws InternalBraaSException {
        URLClassLoader classLoader;
        try {
            classLoader = new DroolsURLClassLoader(compiledClassesDir.toURI().toURL(), KieBase.class.getClassLoader());
            JsonSchemaUtils<URLClassLoader> jsonSchemaUtils = new JsonSchemaUtils<>(classLoader);
            if (schemaFile.exists()) {
                String packageName = schemaFile.getName().replaceAll("-", ".");
                if (schemaFile.isDirectory()) {
                    Iterator<File> files = FileUtils.iterateFilesAndDirs(schemaFile, new SuffixFileFilter(JSON_SUFFIX), TrueFileFilter.INSTANCE);
                    while (files.hasNext()) {
                        File file = files.next();
                        classLoader = jsonSchemaUtils.processFile_(file, packageName, sourceClassesDir, compiledClassesDir);
                    }
                } else if (schemaFile.isFile()) {
                    classLoader = jsonSchemaUtils.processFile_(schemaFile, packageName, sourceClassesDir, compiledClassesDir);
                }
            }
        } catch (URISyntaxException | NoSuchFieldException | IllegalAccessException | IOException e) {
            throw new InternalBraaSException(e);
        }
        return classLoader;
    }

    public static URLClassLoader buildClassesFromSchema(File schemaFile, String fileName, File sourceClassesDir, File compiledClassesDir) throws InternalBraaSException {
        URLClassLoader classLoader;
        try {
            classLoader = new DroolsURLClassLoader(compiledClassesDir.toURI().toURL(), KieBase.class.getClassLoader());
            JsonSchemaUtils<URLClassLoader> jsonSchemaUtils = new JsonSchemaUtils<>(classLoader);
            if (schemaFile.exists()) {
                String packageName = (Objects.requireNonNull(fileName)).replaceAll("-", ".");
                if (schemaFile.isDirectory()) {
                    Iterator<File> files = FileUtils.iterateFilesAndDirs(schemaFile, new SuffixFileFilter(JSON_SUFFIX), TrueFileFilter.INSTANCE);
                    while (files.hasNext()) {
                        File file = files.next();
                        classLoader = jsonSchemaUtils.processFile_(file, packageName, sourceClassesDir, compiledClassesDir);
                    }
                } else if (schemaFile.isFile()) {
                    classLoader = jsonSchemaUtils.processFile_(schemaFile, packageName, sourceClassesDir, compiledClassesDir);
                }
            }
        } catch (URISyntaxException | NoSuchFieldException | IllegalAccessException | IOException e) {
            throw new InternalBraaSException(e);
        }
        return classLoader;
    }

    public static DroolsConfig createDroolsConfig(String rulesPath, URLClassLoader classLoader, DroolsConfig droolsConfig, Constants.URL_RESOURCE_STRATEGIES urlResourceStrategies) throws InternalBraaSException {
        try {
            if (StringUtils.isNotBlank(rulesPath) && droolsConfig == null) {
                Properties props = new PropertiesHandler("drools-absolute-path-decision-table.properties", ApiController.class.getClassLoader()).getProperties();
                props.setProperty("decision.table.path", rulesPath);
                droolsConfig = new DroolsConfig(props);
            }
            droolsConfig.setClassLoader(classLoader);
        } catch (FileNotFoundException | MalformedURLException | URISyntaxException | PropertiesUtilException e) {
            throw new InternalBraaSException(e);
        }
        return droolsConfig;
    }

    public static Collection<?> processAssets(DroolsConfig droolsConfig, URLClassLoader classLoader, Request request) throws Exception {
        Collection<?> results = null;
        if (droolsConfig != null && classLoader != null) {
            DroolsUtils droolsUtils = new DroolsUtils(droolsConfig);
            Map<String, Object> globals = new HashMap<>();
            Locale locale = droolsConfig.getLocale();
            if (locale != null) {
                globals.put("locale", locale);
            } else {
                globals.put("locale", Locale.ENGLISH);
            }
            globals.put("logger", log);
            droolsUtils.addGlobals(globals);
            Object assets = extractAssets(request, classLoader);
            results = getMessages(droolsUtils.executeRules(assets), locale);
        }
        return results;
    }

    public static Object extractAssets(Request request, URLClassLoader classLoader) throws InternalBraaSException {
        Object json;
        try {
            String assetsStr;
            try {
                assetsStr = getStringFromMultipart(request, "assets-file");
            } catch (Throwable ignored) {
                assetsStr = request.body();
            }
            Class[] classes = com.araguacaima.braas.core.Commons.getClassesFromClassLoader(classLoader);
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
            try {
                json = jsonUtils.fromJSON(assetsStr, Collection.class);
                Collection<Object> col = new LinkedList<>();
                Collection<Map<String, Object>> jsonCollection = (Collection<Map<String, Object>>) json;
                bindCollectionAssetToObject(classes, mapper, col, jsonCollection);
                json = col;
            } catch (MismatchedInputException ignored) {
                json = bindSingleAssetToObject(assetsStr, classes, mapper);
            }
        } catch (Throwable t) {
            throw new InternalBraaSException(t);
        }
        return json;
    }

    private static void bindCollectionAssetToObject(Class[] classes, ObjectMapper mapper, Collection<Object> col, Collection<Map<String, Object>> jsonCollection) throws IOException {
        for (Map<String, Object> element : jsonCollection) {
            String element_ = jsonUtils.toJSON(element);
            for (Class<?> clazz : classes) {
                try {
                    Object t = jsonUtils.fromJSON(mapper, element_, clazz);
                    col.add(t);
                    break;
                } catch (Throwable ignored1) {
                }
            }
        }
    }

    private static Object bindSingleAssetToObject(String asset, Class[] classes, ObjectMapper mapper) throws IOException, InternalBraaSException {
        Object json = null;
        for (Class<?> clazz : classes) {
            try {
                json = jsonUtils.fromJSON(mapper, asset, clazz);
                break;
            } catch (MismatchedInputException ignored1) {
            }
        }
        if (json == null) {
            throw new InternalBraaSException("There is no provided schema that satisfy incoming asset. " +
                    "Each asset provided must be compliant with some object definition in provided schema. " +
                    "Is not admissible attempting to use an unrecognized or absent property within any of incoming assets");
        }
        return json;
    }

    public static String extractSchema(Request request, File rulesDir) throws IOException, ServletException {
        String schemaPath;
        try {
            String schema = getStringFromMultipart(request, "schema-json");
            File file = new File(rulesDir, "json-schema.json");
            FileUtils.writeStringToFile(file, schema, StandardCharsets.UTF_8);
            schemaPath = file.getCanonicalPath();
        } catch (Throwable ignored) {
            schemaPath = storeFileAndGetPathFromMultipart(request, "schema-file", rulesDir, BRAAS_RULES_FILE_NAME);
        }
        log.debug("Schema path '" + schemaPath + "' loaded!");
        return schemaPath;
    }

    public static String extractSpreadSheet(Request request, File rulesDir) throws IOException, ServletException {
        String rulesPath = storeFileAndGetPathFromMultipart(request, FILE_NAME_PREFIX, rulesDir, BRAAS_RULES_FILE_NAME);
        log.debug("Rule's base '" + rulesPath + "' loaded!");
        return rulesPath;
    }

    public static Binary extractBinary(Request request) throws InternalBraaSException {
        try {
            return jsonUtils.fromJSON(request.body(), Binary.class);
        } catch (Throwable e) {
            throw new InternalBraaSException(e.getMessage());
        }
    }

    public static String extractSpreadSheetFromBinary(Binary binarySpreadsheet, File rulesDir, String fileName) throws IOException {
        String rulesPath = storeFileAndGetPathFromBinary(binarySpreadsheet, rulesDir, fileName);
        log.debug("Rule's base '" + rulesPath + "' loaded!");
        return rulesPath;
    }

    public static String extractSchemaFromBinary(Binary binarySpreadsheet, File rulesDir) throws InternalBraaSException {
        String schemaPath;
        try {
            String schema;
            Collection schemaArray = binarySpreadsheet.getSchemaArray();
            if (CollectionUtils.isNotEmpty(schemaArray)) {
                schema = jsonUtils.toJSON(schemaArray);
            } else {
                String schema_ = jsonUtils.toJSON(binarySpreadsheet.getSchemaMap());
                if (schema_ == null) {
                    schema = jsonUtils.toJSON(binarySpreadsheet.getSchemas());
                } else {
                    schema = schema_;
                }

            }
            File file = new File(rulesDir, JSON_SCHEMA_FILE_NAME);
            FileUtils.writeStringToFile(file, schema, StandardCharsets.UTF_8);
            schemaPath = file.getCanonicalPath();
        } catch (Throwable t) {
            throw new InternalBraaSException(t);
        }
        log.debug("Schema path '" + schemaPath + "' loaded!");
        return schemaPath;
    }

    public static void setNamespace(Request request, Response response) throws IOException, ServletException {
        String braasSessionId = request.queryParams(BRAAS_SESSION_ID_PARAM);
        final SparkWebContext ctx = new SparkWebContext(request, response);
        String storedSessionId;
        if (braasSessionId != null) {
            ctx.setSessionAttribute(BRAAS_SESSION_ID_PARAM, braasSessionId);
            storedSessionId = braasSessionId;
        } else {
            storedSessionId = (String) ctx.getSessionAttribute(BRAAS_SESSION_ID_PARAM);
        }
        if (org.apache.commons.lang3.StringUtils.isBlank(braasSessionId)) {
            braasSessionId = request.cookie(BRAAS_SESSION_ID_PARAM);
            if (org.apache.commons.lang3.StringUtils.isBlank(braasSessionId)) {
                braasSessionId = UUID.randomUUID().toString();
                response.cookie(BRAAS_SESSION_ID_PARAM, braasSessionId, 86400, true);
            }
        }

        if (org.apache.commons.lang3.StringUtils.isBlank(braasSessionId)) {
            braasSessionId = UUID.randomUUID().toString();
        }
        File tempDir = null;
        if (org.apache.commons.lang3.StringUtils.isNotBlank(storedSessionId)) {
            File baseDir = new File(System.getProperty("java.io.tmpdir"));
            tempDir = new File(baseDir, braasSessionId);
        }

        if (org.apache.commons.lang3.StringUtils.isBlank(storedSessionId) || tempDir == null || !tempDir.exists()) {
            File baseDir = new File(System.getProperty("java.io.tmpdir"));
            tempDir = new File(baseDir, braasSessionId);
            if (!tempDir.exists()) {
                tempDir = FileUtils.createTempDir(braasSessionId);
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
            ctx.setSessionAttribute(BRAAS_SESSION_ID_PARAM, braasSessionId);
        } else {
            if (ctx.getSessionAttribute(UPLOAD_DIR_PARAM) == null) {
                ctx.setSessionAttribute(UPLOAD_DIR_PARAM, new File(tempDir, UPLOAD_DIR));
            }
            if (ctx.getSessionAttribute(RULES_DIR_PARAM) == null) {
                ctx.setSessionAttribute(RULES_DIR_PARAM, new File(tempDir, RULES_DIR));
            }
            if (ctx.getSessionAttribute(SOURCE_CLASSES_DIR_PARAM) == null) {
                ctx.setSessionAttribute(SOURCE_CLASSES_DIR_PARAM, new File(tempDir, SOURCE_CLASSES_DIR));
            }
            if (ctx.getSessionAttribute(COMPILED_CLASSES_DIR_PARAM) == null) {
                ctx.setSessionAttribute(COMPILED_CLASSES_DIR_PARAM, new File(tempDir, COMPILED_CLASSES_DIR));
            }
            ctx.setSessionAttribute(BRAAS_SESSION_ID_PARAM, braasSessionId);
        }
        String contentType = org.apache.commons.lang3.StringUtils.defaultIfBlank(request.headers("Content-Type"), "");
        if (contentType.startsWith("multipart/form-data") || contentType.startsWith("application/x-www-form-urlencoded")) {
            request.raw().setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement);
            request.raw().getParts();
        }
        String body = request.body();
        if (org.apache.commons.lang3.StringUtils.isNotBlank(body)) {
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
    }

    public static class SpreadsheetBaseModel {
        private final SparkWebContext ctx;
        private File rulesDir;
        private File sourceClassesDir;
        private File compiledClassesDir;
        private File uploadDir;

        public SpreadsheetBaseModel(SparkWebContext ctx) {
            this.ctx = ctx;
        }

        public File getRulesDir() {
            return rulesDir;
        }

        public File getSourceClassesDir() {
            return sourceClassesDir;
        }

        public File getCompiledClassesDir() {
            return compiledClassesDir;
        }

        public File getUploadDir() {
            return uploadDir;
        }

        public SpreadsheetBaseModel invoke() {
            rulesDir = (File) ctx.getSessionAttribute(RULES_DIR_PARAM);
            sourceClassesDir = (File) ctx.getSessionAttribute(SOURCE_CLASSES_DIR_PARAM);
            compiledClassesDir = (File) ctx.getSessionAttribute(COMPILED_CLASSES_DIR_PARAM);
            uploadDir = (File) ctx.getSessionAttribute(UPLOAD_DIR_PARAM);
            return this;
        }
    }
}
