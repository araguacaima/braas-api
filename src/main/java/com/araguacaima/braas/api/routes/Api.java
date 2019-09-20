package com.araguacaima.braas.api.routes;

import com.araguacaima.braas.api.BeanBuilder;
import com.araguacaima.braas.api.Server;
import com.araguacaima.braas.api.common.Commons;
import com.araguacaima.braas.api.jsonschema.CustomClassloaderJavaFileManager;
import com.araguacaima.braas.api.jsonschema.PackageClass;
import com.araguacaima.braas.api.jsonschema.RuleFactory;
import com.araguacaima.braas.core.drools.DroolsConfig;
import com.araguacaima.braas.core.drools.DroolsUtils;
import com.araguacaima.commons.utils.*;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.github.victools.jsonschema.generator.Option;
import com.google.common.collect.ImmutableList;
import com.sun.codemodel.JCodeModel;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.jsonschema2pojo.*;
import org.pac4j.sparkjava.SparkWebContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.RouteGroup;

import javax.servlet.http.Part;
import javax.tools.*;
import java.io.*;
import java.net.URI;
import java.nio.charset.Charset;
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
    public static final String BASE = "/base";
    public static final String JSON_SCHEMA = "/json-schema";
    public static final String ASSETS = "/assets";
    public static final String API = "Api";
    public static final String FILE_NAME_PREFIX = "spreadsheet";
    public static final String ZIP_PART_NAME = "zip";
    public static final String JAR_PART_NAME = "jar";
    public static final String ZIP_COMPRESSED_MIME = "application/x-zip-compressed";
    public static final String JAR_MIME = "application/java-archive";
    public static final String OCTET_STREAM_MIME = "application/octet-stream";
    public static final String ZIP_MIME = "application/zip";
    public static final String CLASS_SUFFIX = "class";
    public static final String JSON_SUFFIX = "json";
    public static final String DEFINITIONS_ROOT = "definitions";
    private static Logger log = LoggerFactory.getLogger(Api.class);
    private static GenerationConfig config = new DefaultGenerationConfig() {

        @Override
        public boolean isUsePrimitives() {
            return true;
        }

        @Override
        public boolean isUseLongIntegers() {
            return true;
        }

        @Override
        public AnnotationStyle getAnnotationStyle() {
            return AnnotationStyle.NONE;
        }

        @Override
        public InclusionLevel getInclusionLevel() {
            return InclusionLevel.ALWAYS;
        }

        @Override
        public boolean isUseOptionalForGetters() {
            return false;
        }

        @Override
        public boolean isRemoveOldOutput() {
            return true;
        }

        @Override
        public boolean isSerializable() {
            return true;
        }

        @Override
        public boolean isIncludeConstructors() {
            return true;
        }

        @Override
        public boolean isIncludeAdditionalProperties() {
            return false;
        }

        @Override
        public String getTargetVersion() {
            return "1.8";
        }

        @Override
        public Language getTargetLanguage() {
            return Language.JAVA;
        }

    };
    private static NoopAnnotator noopAnnotator = new NoopAnnotator();
    private static SchemaStore schemaStore = new SchemaStore();
    private static SchemaGenerator schemaGenerator = new SchemaGenerator();

    static {
        classLoaderUtils.init();
        classLoaderUtils.setClassLoader(Api.class.getClassLoader());
    }
    private ZipUtils zipUtils = new ZipUtils();
    private JarUtils jarUtils = new JarUtils();
    private static ClassLoaderUtils classLoaderUtils = new ClassLoaderUtils(null);
    private Collection<Option> with = ImmutableList.of(Option.FLATTENED_ENUMS, Option.SIMPLIFIED_ENUMS, Option.DEFINITIONS_FOR_ALL_OBJECTS);
    private Collection<Option> without = null;

    private FileUtils fileUtils = new FileUtils();

    @Override
    public void addRoutes() {
        //before(Commons.EMPTY_PATH, Commons.genericFilter);
        //before(Commons.EMPTY_PATH, Commons.apiFilter);
        get(Commons.EMPTY_PATH, buildRoute(new BeanBuilder().title(BRAAS + BREADCRUMBS_SEPARATOR + API), "/api"), engine);
        post(BASE, (request, response) -> {
            try {
                final SparkWebContext ctx = new SparkWebContext(request, response);
                File rulesDir = (File) ctx.getSessionAttribute(Server.RULES_DIR_PARAM);
                File sourceClassesDir = (File) ctx.getSessionAttribute(Server.SOURCE_CLASSES_DIR_PARAM);
                File compiledClassesDir = (File) ctx.getSessionAttribute(Server.COMPILED_CLASSES_DIR_PARAM);
                String rulesPath = storeFileAndGetPathFromMultipart(request, FILE_NAME_PREFIX, rulesDir);
                log.debug("Rule's base '" + rulesPath + "' loaded!");
                String schemaPath;
                File schemaFile;
                try {
                    String schema = getStringFromMultipart(request, "schema-json");
                    File file = new File(rulesDir, "schema-json.json");
                    FileUtils.writeStringToFile(file, schema, Charset.forName("UTF-8"));
                    schemaPath = file.getCanonicalPath();
                } catch (Throwable ignored) {
                    schemaPath = storeFileAndGetPathFromMultipart(request, "schema-file", rulesDir);
                }
                log.debug("Schema path '" + schemaPath + "' loaded!");
                ctx.setSessionAttribute("rules-base-file-name", rulesPath);
                schemaFile = new File(schemaPath);
                if (schemaFile.exists()) {
                    String packageName = (Objects.requireNonNull(getFileNameFromPart(request.raw().getPart(FILE_NAME_PREFIX)))).replaceAll("-", ".");
                    if (schemaFile.isDirectory()) {
                        Iterator<File> files = FileUtils.iterateFilesAndDirs(schemaFile, new SuffixFileFilter(JSON_SUFFIX), TrueFileFilter.INSTANCE);
                        while (files.hasNext()) {
                            File file = files.next();
                            processFile(file, packageName, sourceClassesDir, compiledClassesDir);
                        }
                    } else if (schemaFile.isFile()) {
                        processFile(schemaFile, packageName, sourceClassesDir, compiledClassesDir);
                    }
                    if (StringUtils.isNotBlank(rulesPath)) {
                        DroolsConfig droolsConfig = (DroolsConfig) ctx.getSessionAttribute("drools-config");
                        if (droolsConfig == null) {
                            Properties props = new PropertiesHandler("drools-absolute-path-decision-table.properties", this.getClass().getClassLoader()).getProperties();
                            props.setProperty("decision.table.path", rulesPath);
                            droolsConfig = new DroolsConfig(props);
                            ctx.setSessionAttribute("drools-config", droolsConfig);
                        }
                    }
                    response.status(HTTP_CREATED);
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
                        String result = String.join("", Files.readAllLines(filePath));
                        return result;
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
            DroolsConfig droolsConfig = (DroolsConfig) ctx.getSessionAttribute("drools-config");
            DroolsUtils droolsUtils = new DroolsUtils(droolsConfig);
            String assetsStr = request.body();
            Map assets = jsonUtils.fromJSON(assetsStr, Map.class);
            Collection results = droolsUtils.executeRules(assets);
            response.status(HTTP_ACCEPTED);
            response.type(JSON_CONTENT_TYPE);
            return jsonUtils.toJSON(results);
        });
    }

    private void jsonToSourceClassFile(String json, String className, String packageName, File rootDirectory) throws IOException, NoSuchFieldException, IllegalAccessException {
        JCodeModel codeModel = new JCodeModel();
        SchemaMapper mapper = new SchemaMapper(new RuleFactory(config, noopAnnotator, schemaStore, DEFINITIONS_ROOT), schemaGenerator);
        mapper.generate(codeModel, className, packageName, json);
        codeModel.build(rootDirectory);
    }

    public void compileSources(File javaSourcesFile, File outputDirectory) throws IOException, IllegalAccessException, NoSuchFieldException, ClassNotFoundException {
        File[] sourceFiles = org.apache.commons.io.FileUtils.listFiles(javaSourcesFile, new String[]{"java"}, true).toArray(new File[]{});
        JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
        OutputStreamJavaFileManager<JavaFileManager> fileManager =
                new OutputStreamJavaFileManager<>(javaCompiler.getStandardFileManager(null, null, null), outputDirectory);
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager standardJavaFileManager = javaCompiler.getStandardFileManager(diagnostics, null, null);
        final JavaFileManager fileManager1 = new CustomClassloaderJavaFileManager(classLoaderUtils.getClassLoader(), standardJavaFileManager, outputDirectory);

        List<JavaFileObject> fileObjects = new ArrayList<>();
        for (File file : sourceFiles) {
            fileObjects.add(new JavaSourceFromString(file.toURI(), FileUtils.readFileToString(file, Charset.forName("UTF-8"))));
        }
        List<String> options = Arrays.asList("-classpath", classLoaderUtils.getClasspath());

        JavaCompiler.CompilationTask task = javaCompiler.getTask(null, fileManager1, diagnostics, options, null, fileObjects);
        if (!task.call()) {
            StringBuilder errorMsg = new StringBuilder();
            for (Diagnostic d : diagnostics.getDiagnostics()) {
                String err = String.format("Compilation error: Line %d - %s%n", d.getLineNumber(),
                        d.getMessage(null));
                errorMsg.append(err);
                System.err.print(err);
            }
            throw new IOException(errorMsg.toString());
        }
        fileManager.close();
        fileManager.flush();
        classLoaderUtils.addToClasspath(outputDirectory.getCanonicalPath());

        File[] compiledFiles = org.apache.commons.io.FileUtils.listFiles(outputDirectory, new String[]{"class"}, true).toArray(new File[]{});
        for (File file : compiledFiles) {
            String path = file.getCanonicalPath();
            String class_ = fileUtils.getRelativePathFrom(outputDirectory, file).substring(1);
            class_ = class_.replaceAll("/", ".").replaceAll("\\\\", ".") + "." + file.getName().replace(".class", StringUtils.EMPTY);
            classLoaderUtils.addResourceToDependencies(path);
            classLoaderUtils.loadClass(class_);
        }
    }

    private Map getLastValueFromPackageName(String key, Map parentMap) {
        if (StringUtils.isBlank(key)) {
            return parentMap;
        }
        String entry = key.split("\\.")[0];
        Map map = (Map) parentMap.get(entry);
        if (map == null) {
            return null;
        } else {
            String remaining = key.substring(entry.length());
            if (remaining.startsWith(".")) {
                remaining = remaining.substring(1);
            }
            return getLastValueFromPackageName(remaining, map);
        }
    }

    private LinkedHashMap<String, LinkedHashMap> createKeysFromPackageName(String key, LinkedHashMap<String, LinkedHashMap> parentMap) {
        if (StringUtils.isBlank(key)) {
            return new LinkedHashMap<>();
        }
        String entry = key.split("\\.")[0];
        String remaining = key.replaceFirst(entry, StringUtils.EMPTY);
        if (remaining.startsWith(".")) {
            remaining = remaining.substring(1);
        }
        LinkedHashMap<String, LinkedHashMap> map = (LinkedHashMap<String, LinkedHashMap>) parentMap.get(entry);
        if (map == null) {
            map = new LinkedHashMap<>();
            LinkedHashMap<String, LinkedHashMap> value = new LinkedHashMap<>();
            map.put(entry, createKeysFromPackageName(remaining, value));
        } else {
            if (!key.equals(entry)) {
                LinkedHashMap<String, LinkedHashMap> map1 = createKeysFromPackageName(remaining, map);
                if (!map1.equals(map)) {
                    if (!remaining.contains(".")) {
                        map.putAll(map1);
                        return parentMap;
                    } else {
                        map.put(entry, map1);
                    }
                } else {
                    return parentMap;
                }
            } else {
                return parentMap;
            }
        }
        return map;
    }

    private void processFile(File file, String packageName, File sourceFilesDirectory, File compiledFilesDirectory) throws IOException, ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        String json = FileUtils.readFileToString(file, Charset.forName("UTF-8"));
        try {
            Map<String, String> jsonSchema = jsonUtils.fromJSON(json, Map.class);
            String id = jsonSchema.get("$id");
            jsonToSourceClassFile(json, id, packageName, sourceFilesDirectory);
        } catch (MismatchedInputException ignored) {
            Collection<Map<String, Object>> jsonSchemas = jsonUtils.fromJSON(json, Collection.class);
            Set<String> ids = new LinkedHashSet<>();
            LinkedHashMap<String, LinkedHashMap> definitionMap = new LinkedHashMap<>();
            jsonSchemas.forEach(jsonSchema -> {
                try {
                    String id = jsonSchema.get("$id").toString();
                    String className_;
                    String packageName_;
                    if (id.contains(".")) {
                        className_ = id.substring(id.lastIndexOf('.') + 1);
                        packageName_ = id.substring(0, id.lastIndexOf('.'));
                    } else {
                        className_ = id;
                        packageName_ = packageName;
                    }
                    LinkedHashMap<String, LinkedHashMap> map_ = createKeysFromPackageName(packageName_, definitionMap);
                    Map innerMap = getLastValueFromPackageName(packageName_, map_);
                    innerMap.put(className_, jsonSchema);
                    jsonSchema.put("$id", id);
                    jsonSchema.put("$schema", "http://json-schema.org/draft-07/schema#");
                    ids.add(id);
                    definitionMap.putAll(map_);

                    //fixing $ref properties
                    LinkedHashMap properties = (LinkedHashMap) jsonSchema.get("properties");
                    if (MapUtils.isNotEmpty(properties)) {
                        for (Object key : properties.keySet()) {
                            LinkedHashMap value = (LinkedHashMap) properties.get(key);
                            String innerId = (String) value.get("$id");
                            if (StringUtils.isNotBlank(innerId) && innerId.contains(".")) {
                                value.clear();
                                value.put("$ref", "#/definitions/" + innerId.replaceAll("\\.", "/"));
                                LinkedHashMap definitions = (LinkedHashMap) jsonSchema.get(DEFINITIONS_ROOT);
                                if (definitions == null) {
                                    definitions = new LinkedHashMap();
                                    jsonSchema.put(DEFINITIONS_ROOT, definitions);
                                }
                                definitions.put(innerId, "");
                            }
                        }
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            });
            definitionsToClasses(definitionMap, ids, sourceFilesDirectory);
        }
        compileSources(sourceFilesDirectory, compiledFilesDirectory);
    }

    private void definitionsToClasses(LinkedHashMap<String, LinkedHashMap> definitions, Set<String> ids, File rootDirectory) throws IOException, NoSuchFieldException, IllegalAccessException {
        FileUtils.cleanDirectory(rootDirectory);
        for (String id : ids) {
            Map map = getLastValueFromPackageName(id, definitions);
            if (map != null) {
                PackageClass packageClass = new PackageClass(id).invoke();
                String className = packageClass.getClassName();
                String packageName = packageClass.getPackageName();
                LinkedHashMap result = new LinkedHashMap();
                LinkedHashMap map1 = (LinkedHashMap) map.get(DEFINITIONS_ROOT);
                if (map1 != null) {
                    for (Object keyObj : map1.keySet()) {
                        String key = keyObj.toString();
                        Map value = getLastValueFromPackageName(key, definitions);
                        if (value != null) {
                            packageClass = new PackageClass(key).invoke();
                            String className_ = packageClass.getClassName();
                            String packageName_ = packageClass.getPackageName();
                            LinkedHashMap<String, LinkedHashMap> map_ = createKeysFromPackageName(packageName_, result);
                            value.remove("$id");
                            value.remove("$schema");
                            value.remove(DEFINITIONS_ROOT);
                            Map value_ = getLastValueFromPackageName(key, map_);
                            if (value_ == null) {
                                value_ = getLastValueFromPackageName(packageName_, map_);
                                value_.put(className_, value);
                            } else {
                                value_.putAll(value);
                            }
                            result.putAll(map_);
                        }
                    }
                }
                map.put(DEFINITIONS_ROOT, result);
                jsonToSourceClassFile(jsonUtils.toJSON(map), StringUtils.capitalize(className), packageName, rootDirectory);
            }
        }
    }

    private static class OutputStreamSimpleFileObject extends SimpleJavaFileObject {
        private OutputStream outputStream;

        public OutputStreamSimpleFileObject(final URI uri, final JavaFileObject.Kind kind,
                                            final OutputStream outputStream) {
            super(uri, kind);
            this.outputStream = outputStream;
        }

        @Override
        public OutputStream openOutputStream() {
            return this.outputStream;
        }
    }

    private static class OutputStreamJavaFileManager<M extends JavaFileManager>
            extends ForwardingJavaFileManager<M> {
        private File outputDirectory;

        protected OutputStreamJavaFileManager(final M fileManager, final File outputDirectory) {
            super(fileManager);
            this.outputDirectory = outputDirectory;
        }

        @Override
        public JavaFileObject getJavaFileForOutput(final JavaFileManager.Location location,
                                                   final String className, final JavaFileObject.Kind kind, final FileObject sibling) {
            OutputStream outputStream;
            try {
                PackageClass packageClass = PackageClass.instance(className);
                String package_ = packageClass.getPackageName();
                String class_ = packageClass.getClassName();
                File outputFile = FileUtils.makeDirFromPackageName(outputDirectory, package_);
                outputFile = new File(outputFile, class_ + ".class");
                outputStream = new FileOutputStream(outputFile);
            } catch (IOException e) {
                e.printStackTrace();
                outputStream = new ByteArrayOutputStream();
            }
            return new OutputStreamSimpleFileObject(new File(className).toURI(), kind, outputStream);
        }
    }

    private static class JavaSourceFromString extends SimpleJavaFileObject {

        private String code;

        /**
         * Construct a SimpleJavaFileObject of the given kind and with the
         * given URI.
         *
         * @param uri  the URI for this file object
         * @param code Code
         */
        public JavaSourceFromString(URI uri, String code) {
            super(uri, Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return this.code;
        }
    }

}
