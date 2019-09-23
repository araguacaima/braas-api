package com.araguacaima.braas.api.controller;

import com.araguacaima.braas.api.exception.InternalBraaSException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;

import javax.servlet.ServletException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.araguacaima.braas.api.common.Commons.*;
import static com.araguacaima.braas.core.drools.utils.RuleMessageUtils.getMessages;

public class ApiController {

    private static Logger log = LoggerFactory.getLogger(ApiController.class);

    public static URLClassLoader buildClassesFromMultipartJsonSchema_(File schemaFile, String fileNameFromPart, File sourceClassesDir, File compiledClassesDir) throws InternalBraaSException {
        URLClassLoader classLoader;
        try {
            classLoader = new DroolsURLClassLoader(compiledClassesDir.toURI().toURL(), KieBase.class.getClassLoader());
            JsonSchemaUtils<URLClassLoader> jsonSchemaUtils = new JsonSchemaUtils<>(classLoader);
            if (schemaFile.exists()) {
                String packageName = (Objects.requireNonNull(fileNameFromPart)).replaceAll("-", ".");
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

    public static Set<Class<?>> buildClassesFromMultipartJsonSchema(File schemaFile, String fileNameFromPart, File sourceClassesDir, File compiledClassesDir) throws InternalBraaSException {
        Set<Class<?>> classes = null;
        try {
            JsonSchemaUtils jsonSchemaUtils = new JsonSchemaUtils(new DroolsURLClassLoader(compiledClassesDir.toURI().toURL(), KieBase.class.getClassLoader()));
            if (schemaFile.exists()) {
                classes = new LinkedHashSet<>();
                String packageName = (Objects.requireNonNull(fileNameFromPart)).replaceAll("-", ".");
                if (schemaFile.isDirectory()) {
                    Iterator<File> files = FileUtils.iterateFilesAndDirs(schemaFile, new SuffixFileFilter(JSON_SUFFIX), TrueFileFilter.INSTANCE);
                    while (files.hasNext()) {
                        File file = files.next();
                        classes.addAll(jsonSchemaUtils.processFile(file, packageName, sourceClassesDir, compiledClassesDir));
                    }
                } else if (schemaFile.isFile()) {
                    classes.addAll(jsonSchemaUtils.processFile(schemaFile, packageName, sourceClassesDir, compiledClassesDir));
                }
            }
        } catch (URISyntaxException | NoSuchFieldException | IllegalAccessException | IOException e) {
            throw new InternalBraaSException(e);
        }
        return classes;
    }

    public static DroolsConfig createDroolsConfig(String rulesPath, URLClassLoader classLoader, DroolsConfig droolsConfig) throws InternalBraaSException {
        try {
            if (StringUtils.isNotBlank(rulesPath)) {
                if (droolsConfig == null) {
                    Properties props = new PropertiesHandler("drools-absolute-path-decision-table.properties", ApiController.class.getClassLoader()).getProperties();
                    props.setProperty("decision.table.path", rulesPath);
                    droolsConfig = new DroolsConfig(props);
                    droolsConfig.setClassLoader(classLoader);
                }
            } else {
                droolsConfig.setClassLoader(classLoader);
            }
        } catch (FileNotFoundException | MalformedURLException | URISyntaxException | PropertiesUtilException e) {
            throw new InternalBraaSException(e);
        }
        return droolsConfig;
    }

    public static Collection<?> processAssets(DroolsConfig droolsConfig, URLClassLoader classLoader, Locale locale, Request request) throws Exception {
        Collection<?> results = null;
        if (droolsConfig != null && classLoader != null) {
            DroolsUtils droolsUtils = new DroolsUtils(droolsConfig);
            Map<String, Object> globals = new HashMap<>();
            globals.put("locale", locale);
            globals.put("logger", log);
            droolsUtils.addGlobals(globals);
            Object assets = extractAssets(request, classLoader);
            results = getMessages(droolsUtils.executeRules(assets));
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
            schemaPath = storeFileAndGetPathFromMultipart(request, "schema-file", rulesDir);
        }
        log.debug("Schema path '" + schemaPath + "' loaded!");
        return schemaPath;
    }

    public static String extractSpreadSheet(Request request, File rulesDir) throws IOException, ServletException {
        String rulesPath = storeFileAndGetPathFromMultipart(request, FILE_NAME_PREFIX, rulesDir);
        log.debug("Rule's base '" + rulesPath + "' loaded!");
        return rulesPath;
    }

}
