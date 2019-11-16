package com.araguacaima.braas.api.controller;

import com.araguacaima.braas.api.model.BraasDrools;
import com.araguacaima.braas.core.Constants;
import com.araguacaima.braas.core.RuleMessageWarning;
import com.araguacaima.braas.core.drools.DroolsConfig;
import com.araguacaima.braas.core.drools.DroolsURLClassLoader;
import com.araguacaima.braas.core.drools.DroolsUtils;
import com.araguacaima.braas.core.exception.InternalBraaSException;
import com.araguacaima.commons.exception.core.PropertiesUtilException;
import com.araguacaima.commons.utils.FileUtils;
import com.araguacaima.commons.utils.JsonSchemaUtils;
import com.araguacaima.commons.utils.PropertiesHandler;
import com.araguacaima.commons.utils.StringUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
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
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.araguacaima.braas.api.Server.multipartConfigElement;
import static com.araguacaima.braas.api.common.Commons.*;
import static com.araguacaima.braas.core.Commons.reflectionUtils;

@SuppressWarnings({"unchecked", "SuspiciousMethodCalls"})
public class ApiController {

    private static Logger log = LoggerFactory.getLogger(ApiController.class);

    public static URLClassLoader buildClassesFromSchemaFile(BraasDrools braasDrools, File sourceClassesDir, File compiledClassesDir) throws InternalBraaSException {
        try {
            Collection col = braasDrools.getSchemaArray();
            if (col != null) {
                return buildClassesFromSchemaFile(jsonUtils.toJSON(col), sourceClassesDir, compiledClassesDir);
            }
            Map map = braasDrools.getSchemaMap();
            if (map != null) {
                return buildClassesFromSchemaFile(jsonUtils.toJSON(map), sourceClassesDir, compiledClassesDir);
            }
            throw new InternalBraaSException("Incompatible schemas");
        } catch (IOException e) {
            throw new InternalBraaSException(e);
        }
    }

    private static URLClassLoader buildClassesFromSchemaFile(String json, File sourceClassesDir, File compiledClassesDir) throws InternalBraaSException {
        URLClassLoader classLoader;
        try {
            classLoader = new DroolsURLClassLoader(compiledClassesDir.toURI().toURL(), KieBase.class.getClassLoader());
            JsonSchemaUtils<URLClassLoader> jsonSchemaUtils = new JsonSchemaUtils<>(classLoader);
            if (StringUtils.isNotBlank(json)) {
                classLoader = jsonSchemaUtils.processFile_(json, null, sourceClassesDir, compiledClassesDir);
            }
        } catch (URISyntaxException | NoSuchFieldException | IllegalAccessException | IOException | InstantiationException e) {
            throw new InternalBraaSException(e);
        }
        return classLoader;
    }

    public static URLClassLoader buildClassesFromSchemaStr(String schemaStr, String fileName, File sourceClassesDir, File compiledClassesDir) throws InternalBraaSException {
        URLClassLoader classLoader;
        try {
            classLoader = new DroolsURLClassLoader(compiledClassesDir.toURI().toURL(), KieBase.class.getClassLoader());
            JsonSchemaUtils<URLClassLoader> jsonSchemaUtils = new JsonSchemaUtils<>(classLoader);
            if (StringUtils.isNotEmpty(schemaStr)) {
                String packageName = (Objects.requireNonNull(fileName)).replaceAll("-", ".").replaceAll(" ", ".").toLowerCase();
                classLoader = jsonSchemaUtils.processFile_(schemaStr, packageName, sourceClassesDir, compiledClassesDir);
            }
        } catch (URISyntaxException | NoSuchFieldException | IllegalAccessException | IOException | InstantiationException e) {
            throw new InternalBraaSException(e);
        }
        return classLoader;
    }

    public static URLClassLoader buildClassesFromSchemaFile(File schemaFile, String fileName, File sourceClassesDir, File compiledClassesDir) throws InternalBraaSException {
        URLClassLoader classLoader;
        try {
            classLoader = new DroolsURLClassLoader(compiledClassesDir.toURI().toURL(), KieBase.class.getClassLoader());
            JsonSchemaUtils<URLClassLoader> jsonSchemaUtils = new JsonSchemaUtils<>(classLoader);
            if (schemaFile.exists()) {
                String packageName = (Objects.requireNonNull(fileName)).replaceAll("-", ".").replaceAll(" ", ".").toLowerCase();
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
        } catch (URISyntaxException | NoSuchFieldException | IllegalAccessException | IOException | InstantiationException e) {
            throw new InternalBraaSException(e);
        }
        return classLoader;
    }

    public static DroolsConfig createDroolsConfig(String base64BinarySpreadsheet, URLClassLoader classLoader, DroolsConfig droolsConfig, Constants.URL_RESOURCE_STRATEGIES urlResourceStrategies) throws InternalBraaSException {
        try {
            if (StringUtils.isNotBlank(base64BinarySpreadsheet) && droolsConfig == null) {
                Properties props;
                switch (urlResourceStrategies) {
                    case ABSOLUTE_DECISION_TABLE_PATH:
                        props = new PropertiesHandler("drools-absolute-path-decision-table.properties", ApiController.class.getClassLoader()).getProperties();
                        break;
                    default:
                        props = new Properties();
                }
                droolsConfig = new DroolsConfig(props);
                droolsConfig.setSpreadsheetStreamFromString(base64BinarySpreadsheet);
                droolsConfig.setUrlResourceStrategy(Constants.URL_RESOURCE_STRATEGIES.STREAM_DECISION_TABLE.name());
            }
            droolsConfig.setClassLoader(classLoader);
        } catch (URISyntaxException | PropertiesUtilException | IOException e) {
            throw new InternalBraaSException(e);
        }
        return droolsConfig;
    }

    public static Collection<?> processAssets(DroolsConfig droolsConfig, Request request) throws Exception {

        Collection result = null;
        if (droolsConfig != null) {
            result = new HashSet();
            Collection<?> intermediateResult;
            DroolsUtils droolsUtils = new DroolsUtils(droolsConfig);
            Map<String, Object> globals = new HashMap<>();
            Locale locale = droolsConfig.getLocale();

            if (locale != null) {
                log.info("Drools config locale: " + locale.getLanguage());
                globals.put("locale", locale.getLanguage());
                log.info("Locale set: " + globals.get("locale"));
            }
            globals.put("logger", log);
            droolsUtils.addGlobals(globals);
            Object assets = extractAssets(request, droolsConfig.getClassLoader());
            intermediateResult = droolsUtils.executeRules(assets);
            if (reflectionUtils.isCollectionImplementation(assets.getClass())) {
                intermediateResult.removeAll((Collection) assets);
            } else {
                intermediateResult.remove(assets);
            }

            for (Object element : intermediateResult) {
                if (reflectionUtils.isCollectionImplementation(element.getClass())) {
                    result.addAll((Collection) element);
                } else {
                    result.add(element);
                }
            }

            for (String globalIdentifier : droolsUtils.getGlobalsFromRules()) {
                Object globalToBeAdded = droolsUtils.getGlobals().get(globalIdentifier);
                if (globalToBeAdded != null && !globalToBeAdded.getClass().equals(Object.class)) {
                    Class clazz = globalToBeAdded.getClass();
                    if (reflectionUtils.isCollectionImplementation(clazz)) {
                        Collection globalToBeAdded1 = (Collection) globalToBeAdded;
                        if (!globalToBeAdded1.isEmpty()) {
                            result.addAll(globalToBeAdded1);
                        }
                    } else if (reflectionUtils.isMapImplementation(clazz)) {
                        Map globalToBeAdded1 = (Map) globalToBeAdded;
                        if (!globalToBeAdded1.isEmpty()) {
                            result.add(globalToBeAdded);
                        }
                    } else {
                        HashMap hashMap = new HashMap();
                        hashMap.put(globalIdentifier, globalToBeAdded);
                        result.add(hashMap);
                    }
                }
            }
        }
        return result;
    }

    private static Object extractAssets(Request request, URLClassLoader classLoader) throws InternalBraaSException {
        Object json;
        try {
            String assetsStr;
            try {
                assetsStr = getStringFromMultipart(request, "assets-file");
            } catch (Throwable ignored) {
                assetsStr = request.body();
            }
            log.info("Incoming assets: " + assetsStr);
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
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            String element_ = jsonUtils.toJSON(mapper, element);
            for (Class<?> clazz : classes) {
                try {
                    Object t = jsonUtils.fromJSON(mapper, element_, clazz);
                    col.add(t);
                    break;
                } catch (Throwable t) {
                    log.info("Incoming string can not be bind to class '" + clazz.getName() + "' due Exception: " + t.getMessage());
                }
            }
        }
        if (col.isEmpty()) {
            String language = "en";
            String comment = "No incoming asset is bindable to any provided model definition within schemas";
            String expectedValue = "Some asset whose structure match with some model definition within any of provided schemas";
            col.add(new RuleMessageWarning(language, null, comment, expectedValue, null, null, jsonCollection));
            language = "es";
            comment = "Ninguna entrada es asociable a alguna definición del modelo dentro de los esquemas provistos";
            expectedValue = "Alguna entrada cuya estructura se corresponda con alguna definición de un modelo dentro de los esquemas provistos";
            col.add(new RuleMessageWarning(language, null, comment, expectedValue, null, null, jsonCollection));
        }
    }

    private static Object bindSingleAssetToObject(String asset, Class[] classes, ObjectMapper mapper) throws IOException, InternalBraaSException {
        Object json = null;
        Class foundClass = null;
        for (Class<?> clazz : classes) {
            try {
                json = jsonUtils.fromJSON(mapper, asset, clazz);
                foundClass = clazz;
                break;
            } catch (MismatchedInputException ignored1) {
                log.warn(ignored1.getMessage());
            }
        }
        if (json == null) {
            throw new InternalBraaSException("There is no provided schema that satisfy incoming asset. " +
                    "Each asset provided must be compliant with some object definition in provided schema. " +
                    "Is not admissible attempting to use an unrecognized or absent property within any of incoming assets");
        } else {
            log.info("Incoming asset '" + asset + "' binded to type '" + foundClass.getName() + "'");
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
            schemaPath = storeFileAndGetPathFromMultipart(request, "schema-file", rulesDir, JSON_SCHEMA_FILE_NAME);
        }
        log.debug("Schema path '" + schemaPath + "' loaded!");
        return schemaPath;
    }

    public static String extractSpreadSheet(Request request, File rulesDir) throws IOException, ServletException {
        String rulesPath = storeFileAndGetPathFromMultipart(request, FILE_NAME_PREFIX, rulesDir, BRAAS_RULES_FILE_NAME);
        log.debug("Rule's base '" + rulesPath + "' loaded!");
        return rulesPath;
    }

    public static BraasDrools extractBinary(Request request) throws InternalBraaSException {
        try {
            return jsonUtils.fromJSON(request.body(), BraasDrools.class);
        } catch (Throwable e) {
            throw new InternalBraaSException(e.getMessage());
        }
    }

    public static String extractSpreadSheetFromBinary(BraasDrools braasDroolsSpreadsheet, File rulesDir, String fileName) throws IOException {
        String rulesPath = storeFileAndGetPathFromBinary(braasDroolsSpreadsheet, rulesDir, fileName);
        log.debug("Rule's base '" + rulesPath + "' loaded!");
        return rulesPath;
    }

    public static String extractSchemaFromBinary(BraasDrools braasDrools, File rulesDir) throws InternalBraaSException {
        String schemaPath;
        try {
            String schema;
            Collection schemaArray = braasDrools.getSchemaArray();
            if (CollectionUtils.isNotEmpty(schemaArray)) {
                schema = jsonUtils.toJSON(schemaArray);
            } else {
                String schema_ = jsonUtils.toJSON(braasDrools.getSchemaMap());
                if (schema_ == null) {
                    schema = jsonUtils.toJSON(braasDrools.getSchemas());
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

    private static BraasDrools fixNamespace(String braasSessionId) {
        BraasDrools braasDrools = MongoAccess.getBraasDroolsById(BRAAS_DROOLS_PARAM, braasSessionId);
        if (braasDrools == null) {
            BraasDrools newBraasDrools = new BraasDrools();
            newBraasDrools.setBraasId(braasSessionId);
            braasDrools = MongoAccess.storeBraasDrools(BRAAS_DROOLS_PARAM, newBraasDrools);
        }
        return braasDrools;
    }

    public static void setLocalEnvironment(Request request, Response response) throws IOException, ServletException {
        String braasSessionId = request.queryParams(BRAAS_SESSION_ID_PARAM);
        if (StringUtils.isBlank(braasSessionId)) {
            braasSessionId = request.params(BRAAS_SESSION_ID_PARAM);
        }
        final SparkWebContext ctx = new SparkWebContext(request, response);
        if (braasSessionId == null) {
            braasSessionId = (String) ctx.getSessionAttribute(BRAAS_SESSION_ID_PARAM);
            if (braasSessionId == null) {
                braasSessionId = UUID.randomUUID().toString();
            }
        }
        ctx.setSessionAttribute(BRAAS_SESSION_ID_PARAM, braasSessionId);
        enableMultipart(request);
    }

    public static void setNamespace(Request request, Response response) throws IOException, ServletException {
        String braasSessionId = request.queryParams(BRAAS_SESSION_ID_PARAM);
        if (StringUtils.isBlank(braasSessionId)) {
            braasSessionId = request.params(BRAAS_SESSION_ID_PARAM);
        }
        final SparkWebContext ctx = new SparkWebContext(request, response);
        BraasDrools braasDrools;
        if (braasSessionId != null) {
            braasDrools = buildBraasDrools(braasSessionId, ctx);
        } else {
            braasSessionId = (String) ctx.getSessionAttribute(BRAAS_SESSION_ID_PARAM);
            if (braasSessionId == null) {
                braasSessionId = UUID.randomUUID().toString();
            }
            ctx.setSessionAttribute(BRAAS_SESSION_ID_PARAM, braasSessionId);
            braasDrools = (BraasDrools) ctx.getSessionAttribute(BRAAS_DROOLS_PARAM);
            if (braasDrools == null) {
                braasDrools = fixNamespace(braasSessionId);
            }
        }
        ctx.setSessionAttribute(BRAAS_DROOLS_PARAM, braasDrools);
        enableMultipart(request);
    }

    private static void enableMultipart(Request request) throws IOException, ServletException {
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

    public static BraasDrools buildBraasDrools(String braasSessionId, SparkWebContext ctx) {
        BraasDrools braasDrools;
        String storedBraasSessionId = (String) ctx.getSessionAttribute(BRAAS_SESSION_ID_PARAM);
        if (!braasSessionId.equals(storedBraasSessionId)) {
            ctx.setSessionAttribute(BRAAS_SESSION_ID_PARAM, braasSessionId);
            braasDrools = fixNamespace(braasSessionId);
        } else {
            braasDrools = (BraasDrools) ctx.getSessionAttribute(BRAAS_DROOLS_PARAM);
            if (braasDrools == null) {
                braasDrools = fixNamespace(braasSessionId);
            }
        }
        return braasDrools;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static class RuleBaseModel {
        private final SparkWebContext ctx;
        private File rulesDir;
        private File sourceClassesDir;
        private File compiledClassesDir;

        public RuleBaseModel(SparkWebContext ctx) {
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

        public RuleBaseModel invoke() {
            String braasSessionId = (String) ctx.getSessionAttribute(BRAAS_SESSION_ID_PARAM);
            File tempFile = new File(System.getProperty("java.io.tmpdir"), braasSessionId);
            if (!tempFile.exists()) {
                tempFile = FileUtils.createTempDir(braasSessionId);
            }
            rulesDir = new File(tempFile, RULES_DIR);
            rulesDir.mkdir();
            rulesDir.setReadable(true);
            rulesDir.setWritable(true);
            rulesDir.deleteOnExit();

            sourceClassesDir = new File(tempFile, SOURCE_CLASSES_DIR);
            sourceClassesDir.mkdir();
            sourceClassesDir.setReadable(true);
            sourceClassesDir.setWritable(true);
            sourceClassesDir.deleteOnExit();

            compiledClassesDir = new File(tempFile, COMPILED_CLASSES_DIR);
            compiledClassesDir.mkdir();
            compiledClassesDir.setReadable(true);
            compiledClassesDir.setWritable(true);
            compiledClassesDir.deleteOnExit();

            return this;
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static class UploadModel {
        private final SparkWebContext ctx;
        private File uploadDir;

        public UploadModel(SparkWebContext ctx) {
            this.ctx = ctx;
        }


        public File getUploadDir() {
            return uploadDir;
        }

        public UploadModel invoke() {
            String braasSessionId = (String) ctx.getSessionAttribute(BRAAS_SESSION_ID_PARAM);
            File tempFile = new File(System.getProperty("java.io.tmpdir"), braasSessionId);
            if (!tempFile.exists()) {
                tempFile = FileUtils.createTempDir(braasSessionId);
            }


            uploadDir = new File(tempFile, UPLOAD_DIR);
            uploadDir.mkdir();
            uploadDir.setReadable(true);
            uploadDir.setWritable(true);
            uploadDir.deleteOnExit();

            return this;
        }
    }
}
