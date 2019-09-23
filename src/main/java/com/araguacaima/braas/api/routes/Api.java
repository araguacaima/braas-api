package com.araguacaima.braas.api.routes;

import com.araguacaima.braas.api.BeanBuilder;
import com.araguacaima.braas.api.Server;
import com.araguacaima.braas.api.common.Commons;
import com.araguacaima.braas.api.controller.ApiController;
import com.araguacaima.braas.core.drools.DroolsConfig;
import com.araguacaima.commons.utils.FileUtils;
import com.araguacaima.commons.utils.JarUtils;
import com.araguacaima.commons.utils.StringUtils;
import com.araguacaima.commons.utils.ZipUtils;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static com.araguacaima.braas.api.Server.engine;
import static com.araguacaima.braas.api.common.Commons.*;
import static java.net.HttpURLConnection.*;
import static spark.Spark.get;
import static spark.Spark.post;

public class Api implements RouteGroup {

    public static final String PATH = "/api";
    private static final String BASE = "/base";
    public static final String JSON_SCHEMA = "/json-schema";
    private static final String ASSETS = "/assets";
    public static final String API = "Api";
    private static final String ZIP_COMPRESSED_MIME = "application/x-zip-compressed";
    private static final String JAR_MIME = "application/java-archive";
    private static final String OCTET_STREAM_MIME = "application/octet-stream";
    private static final String ZIP_MIME = "application/zip";

    private static Logger log = LoggerFactory.getLogger(Api.class);

    private ZipUtils zipUtils = new ZipUtils();
    private JarUtils jarUtils = new JarUtils();

    private Collection<Option> with = ImmutableList.of(Option.FLATTENED_ENUMS, Option.SIMPLIFIED_ENUMS, Option.DEFINITIONS_FOR_ALL_OBJECTS);
    private Collection<Option> without = null;

    @Override
    public void addRoutes() {
        //before(Commons.EMPTY_PATH, Commons.genericFilter);
        //before(Commons.EMPTY_PATH, Commons.apiFilter);
        get(Commons.EMPTY_PATH, buildRoute(new BeanBuilder().title(BRAAS + BREADCRUMBS_SEPARATOR + API), "/api"), engine);
        post(BASE, (request, response) -> {
            try {
                final SparkWebContext ctx = new SparkWebContext(request, response);
                SpreadsheetBaseModel spreadsheetBaseModel = new SpreadsheetBaseModel(ctx).invoke();
                File rulesDir = spreadsheetBaseModel.getRulesDir();
                File sourceClassesDir = spreadsheetBaseModel.getSourceClassesDir();
                File compiledClassesDir = spreadsheetBaseModel.getCompiledClassesDir();
                String rulesPath;
                try {
                    rulesPath = ApiController.extractSpreadSheet(request, rulesDir);
                } catch (Throwable t) {
                    return Commons.throwError(response, HTTP_CONFLICT, new Exception("Rule base spreadsheet is not present on request. Make sure you provide it according to API specification [http://braaservice.com/api#/Rules_base/add_base]", t));
                }
                String schemaPath;
                try {
                    schemaPath = ApiController.extractSchema(request, rulesDir);
                    ctx.setSessionAttribute("rules-base-file-name", rulesPath);
                } catch (Throwable t) {
                    return Commons.throwError(response, HTTP_CONFLICT, new Exception("Json schema is not present on request. Make sure you provide it according to API specification [http://braaservice.com/api#/Rules_base/add_base]", t));
                }
                File schemaFile = new File(schemaPath);
                URLClassLoader classLoader = ApiController.buildClassesFromMultipartJsonSchema_(
                        schemaFile, getFileNameFromPart(request.raw().getPart(FILE_NAME_PREFIX)), sourceClassesDir, compiledClassesDir);
                if (classLoader != null) {
                    ctx.setSessionAttribute("drools-config", ApiController.createDroolsConfig(
                            rulesPath, classLoader, (DroolsConfig) ctx.getSessionAttribute("drools-config")));
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
        get(BASE, (request, response) -> {
            final SparkWebContext ctx = new SparkWebContext(request, response);
            String fileName = (String) ctx.getSessionAttribute("rules-base-file-name");
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
        post(JSON_SCHEMA, (request, response) -> {
            final SparkWebContext ctx = new SparkWebContext(request, response);
            File uploadDir = (File) ctx.getSessionAttribute(Server.UPLOAD_DIR_PARAM);
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
                        folderName = file.getName().split("\\.")[0];
                        destinationDir = uploadDir.getCanonicalPath() + File.separator + folderName;
                        //noinspection ResultOfMethodCallIgnored
                        new File(destinationDir).delete();
                        zipUtils.unZip(file, uploadDir);
                    }
                } catch (Throwable ignored) {
                    classesPath = storeFileAndGetPathFromMultipart(request, JAR_PART_NAME, uploadDir);
                    Part part = request.raw().getPart(JAR_PART_NAME);
                    String contentType = part.getContentType();
                    if (JAR_MIME.equalsIgnoreCase(contentType) || OCTET_STREAM_MIME.equalsIgnoreCase(contentType)) {
                        File file = new File(classesPath);
                        folderName = file.getName().split("\\.")[0];
                        destinationDir = uploadDir.getCanonicalPath() + File.separator + folderName;
                        //noinspection ResultOfMethodCallIgnored
                        new File(destinationDir).delete();
                        jarUtils.unZip(destinationDir, file.getCanonicalPath());
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
        post(ASSETS, (request, response) -> {
            final SparkWebContext ctx = new SparkWebContext(request, response);
            Locale locale = Locale.ENGLISH;
            String localeStr = request.queryParams("locale");
            try {
                if (StringUtils.isNotBlank(localeStr)) {
                    locale = LocaleUtils.toLocale(localeStr);
                }
            } catch (IllegalArgumentException ignored) {

            }
            SpreadsheetBaseModel spreadsheetBaseModel = new SpreadsheetBaseModel(ctx).invoke();
            File rulesDir = spreadsheetBaseModel.getRulesDir();
            File sourceClassesDir = spreadsheetBaseModel.getSourceClassesDir();
            File compiledClassesDir = spreadsheetBaseModel.getCompiledClassesDir();
            String rulesPath = null;
            DroolsConfig droolsConfig = null;
            try {
                rulesPath = ApiController.extractSpreadSheet(request, rulesDir);
            } catch (Throwable t) {
                droolsConfig = (DroolsConfig) ctx.getSessionAttribute("drools-config");
                if (droolsConfig == null) {
                    return Commons.throwError(response, 424, new Exception("Rule base spreadsheet is not present on request not previously provided. Make sure you provide it according to API specification [http://braaservice.com/api#/Rules_base/add_base]", t));
                }
                droolsConfig.setLocale(locale);
            }
            URLClassLoader classLoader;
            if (droolsConfig == null && StringUtils.isNotBlank(rulesPath)) {
                String schemaPath;
                try {
                    schemaPath = ApiController.extractSchema(request, rulesDir);
                    ctx.setSessionAttribute("rules-base-file-name", rulesPath);
                } catch (Throwable t) {
                    return Commons.throwError(response, HTTP_CONFLICT, new Exception("Json schema is not present on request. Make sure you provide it according to API specification [http://braaservice.com/api#/Rules_base/add_base]", t));
                }
                File schemaFile = new File(schemaPath);
                classLoader = ApiController.buildClassesFromMultipartJsonSchema_(
                        schemaFile, getFileNameFromPart(request.raw().getPart(FILE_NAME_PREFIX)), sourceClassesDir, compiledClassesDir);
                if (classLoader != null) {
                    droolsConfig = ApiController.createDroolsConfig(rulesPath, classLoader, (DroolsConfig) ctx.getSessionAttribute("drools-config"));
                    droolsConfig.setLocale(locale);
                    ctx.setSessionAttribute("drools-config", droolsConfig);
                    Collection<?> results = ApiController.processAssets(droolsConfig, classLoader, request);
                    classLoader.close();
                    if (results != null) {
                        response.status(HTTP_ACCEPTED);
                        response.type(JSON_CONTENT_TYPE);
                        return jsonUtils.toJSON(results);
                    }
                } else {
                    return Commons.throwError(response, HTTP_INTERNAL_ERROR, new Exception("It was not possible to load your provided schema to be used later in your rule's base"));
                }
            }
            return Commons.throwError(response, HTTP_INTERNAL_ERROR, new Exception("It was not possible to load your provided schema to be used later in your rule's base"));
        });
    }

    private static class SpreadsheetBaseModel {
        private SparkWebContext ctx;
        private File rulesDir;
        private File sourceClassesDir;
        private File compiledClassesDir;

        SpreadsheetBaseModel(SparkWebContext ctx) {
            this.ctx = ctx;
        }

        File getRulesDir() {
            return rulesDir;
        }

        File getSourceClassesDir() {
            return sourceClassesDir;
        }

        File getCompiledClassesDir() {
            return compiledClassesDir;
        }

        public SpreadsheetBaseModel invoke() {
            rulesDir = (File) ctx.getSessionAttribute(Server.RULES_DIR_PARAM);
            sourceClassesDir = (File) ctx.getSessionAttribute(Server.SOURCE_CLASSES_DIR_PARAM);
            compiledClassesDir = (File) ctx.getSessionAttribute(Server.COMPILED_CLASSES_DIR_PARAM);
            return this;
        }
    }
}
