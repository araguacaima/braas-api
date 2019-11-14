package com.araguacaima.braas.api.controller.jsonschema;

import com.araguacaima.commons.utils.*;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.jsonschema2pojo.*;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@SuppressWarnings("UnusedReturnValue")
public class JsonSchemaUtils<T extends ClassLoader> extends com.araguacaima.commons.utils.JsonSchemaUtils<T> {

    public static final String DEFINITIONS_ROOT = "definitions";
    private static final NoopAnnotator noopAnnotator = new NoopAnnotator();
    private static final SchemaStore schemaStore = new SchemaStore();
    private static final org.jsonschema2pojo.SchemaGenerator schemaGenerator = new org.jsonschema2pojo.SchemaGenerator();
    private static final GenerationConfig config = new DefaultGenerationConfig() {

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
    private static ReflectionUtils reflectionUtils = ReflectionUtils.getInstance();
    private final JsonUtils jsonUtils = new JsonUtils();
    private final MapUtils mapUtils = MapUtils.getInstance();
    private CompilerUtils.FilesCompiler<T> filesCompiler;

    public JsonSchemaUtils(T classLoader) {
        super(classLoader);
    }


    public T processFile_(File file, String packageName, File sourceCodeDirectory, File compiledClassesDirectory) throws IOException, NoSuchFieldException, IllegalAccessException, URISyntaxException, InstantiationException {
        processFile(file, packageName, sourceCodeDirectory, compiledClassesDirectory);
        return filesCompiler.getClassLoader();
    }

    public T processFile_(String json, String packageName, File sourceCodeDirectory, File compiledClassesDirectory) throws IOException, NoSuchFieldException, IllegalAccessException, URISyntaxException, InstantiationException {
        processFile(json, packageName, sourceCodeDirectory, compiledClassesDirectory);
        return filesCompiler.getClassLoader();
    }

    public Set<Class<?>> processFile(File file, String packageName, File sourceCodeDirectory, File compiledClassesDirectory) throws IOException, NoSuchFieldException, IllegalAccessException, URISyntaxException, InstantiationException {
        String json = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        return processFile(json, packageName, sourceCodeDirectory, compiledClassesDirectory);
    }

    @SuppressWarnings("unchecked")
    public Set<Class<?>> processFile(String json, String packageName, File sourceCodeDirectory, File compiledClassesDirectory) throws IOException, NoSuchFieldException, IllegalAccessException, URISyntaxException, InstantiationException {
        try {
            FileUtils.cleanDirectory(sourceCodeDirectory);
            Map jsonSchema = jsonUtils.fromJSON(json, Map.class);
            String id = String.valueOf(jsonSchema.get("$id"));
            String className_;
            String packageName_;
            if (id.contains(".")) {
                className_ = id.substring(id.lastIndexOf('.') + 1);
                packageName_ = id.substring(0, id.lastIndexOf('.'));
            } else {
                className_ = id;
                packageName_ = packageName;
            }
            LinkedHashMap<String, LinkedHashMap> definitionMap = new LinkedHashMap<>();
            Set<String> ids = new LinkedHashSet<>();
            buildDefinitions(packageName, ids, definitionMap, (Map<String, Object>) jsonSchema.get(DEFINITIONS_ROOT));
            RuleFactory ruleFactory = new RuleFactory(config, noopAnnotator, schemaStore, DEFINITIONS_ROOT, definitionMap);
            jsonUtils.jsonToSourceClassFile(json, className_, packageName_, sourceCodeDirectory, ruleFactory, schemaGenerator);
        } catch (MismatchedInputException ignored) {
            Collection<Map<String, Object>> jsonSchemas = jsonUtils.fromJSON(json, Collection.class);
            Set<String> ids = new LinkedHashSet<>();
            LinkedHashMap<String, LinkedHashMap> definitionMap = new LinkedHashMap<>();
            jsonSchemas.forEach(jsonSchema -> buildDefinitions(packageName, ids, definitionMap, jsonSchema));
            deleteInnerDefinitions(definitionMap);
            definitionsToClasses(definitionMap, ids, sourceCodeDirectory);
        }
        return filesCompiler.compile(sourceCodeDirectory, compiledClassesDirectory, org.apache.commons.io.FileUtils.listFiles(sourceCodeDirectory, new String[]{"java"}, true));
    }

    private void buildDefinitions(String packageName, Set<String> ids, LinkedHashMap<String, LinkedHashMap> definitionMap, Map<String, Object> jsonSchema) {
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
            LinkedHashMap<String, LinkedHashMap> map_ = mapUtils.createKeysFromPackageName(packageName_, definitionMap);
            Map innerMap = mapUtils.getLastValueFromPackageName(packageName_, map_);
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
                        String type;
                        value.clear();
                        if (reflectionUtils.isCollectionImplementation(innerId)) {
                            type = reflectionUtils.getExtractedGenerics(innerId);
                            innerId = reflectionUtils.returnNativeClass(innerId);
                            value.put("type", "array");
                            Map map__ = new HashMap();
                            map__.put("$ref", "#/definitions/" + type.replaceAll("\\.", "/"));
                            value.put("items", map__);
                            boolean uniqueItems = false;
                            try {
                                Class clazz = Class.forName(innerId);
                                uniqueItems = Set.class.isAssignableFrom(clazz);
                            } catch (Throwable ignored) {
                            }
                            value.put("uniqueItems", uniqueItems);
                        } else {
                            type = innerId;
                            value.put("$ref", "#/definitions/" + type.replaceAll("\\.", "/"));
                        }
                        LinkedHashMap definitions = (LinkedHashMap) jsonSchema.get(com.araguacaima.commons.utils.JsonSchemaUtils.DEFINITIONS_ROOT);
                        if (definitions == null) {
                            definitions = new LinkedHashMap();
                            jsonSchema.put(com.araguacaima.commons.utils.JsonSchemaUtils.DEFINITIONS_ROOT, definitions);
                        }
                        definitions.put(type, "");
                    }
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void definitionsToClasses(LinkedHashMap<String, LinkedHashMap> definitions, Set<String> ids, File rootDirectory) throws IOException, NoSuchFieldException, IllegalAccessException, InstantiationException {
        FileUtils.cleanDirectory(rootDirectory);
        RuleFactory ruleFactory = new RuleFactory(config, noopAnnotator, schemaStore, DEFINITIONS_ROOT, definitions);
        for (String id : ids) {
            Map map_ = mapUtils.getLastValueFromPackageName(id, definitions);
            if (map_ != null) {
                Map map = new LinkedHashMap<>(mapUtils.traverseAndCreateNew(map_));
                PackageClassUtils packageClassUtils = PackageClassUtils.instance(id);
                String className = packageClassUtils.getClassName();
                String packageName = packageClassUtils.getPackageName();
                Map filteredDefinitions = filterSchema(definitions, id);
                map.put(DEFINITIONS_ROOT, filteredDefinitions);
                String json = jsonUtils.toJSON(map);
                jsonUtils.jsonToSourceClassFile(json, StringUtils.capitalize(className), packageName, rootDirectory, ruleFactory, schemaGenerator);
            }
        }
    }

    private LinkedHashMap<String, LinkedHashMap> filterSchema(LinkedHashMap<String, LinkedHashMap> definitions, String id) throws InstantiationException, IllegalAccessException {
        LinkedHashMap<String, LinkedHashMap> map = new LinkedHashMap<>(mapUtils.traverseAndCreateNew(definitions));
        PackageClassUtils packageClassUtils = PackageClassUtils.instance(id);
        String className = packageClassUtils.getClassName();
        String packageName = packageClassUtils.getPackageName();
        Map innerMap = mapUtils.getLastValueFromPackageName(packageName, map);
        innerMap.remove(className);
        return map;
    }

    private void deleteInnerDefinitions(Map originMap) {
        if (originMap != null) {
            for (Object key : originMap.keySet()) {
                Object value = originMap.get(key);
                if (Map.class.isAssignableFrom(value.getClass())) {
                    Map value1 = (Map) value;
                    Object type = value1.get("type");
                    if (type != null) {
                        if (String.class.isAssignableFrom(type.getClass())) {
                            if ("object".equals(type)) {
                                value1.remove("definitions");
                            } else {
                                Object properties = value1.get("properties");
                                if (properties != null) {
                                    if (Map.class.isAssignableFrom(properties.getClass())) {
                                        value1.remove("definitions");
                                    }
                                } else {
                                    deleteInnerDefinitions(value1);
                                }
                            }
                        }
                    } else {
                        Object properties = value1.get("properties");
                        if (properties != null) {
                            if (Map.class.isAssignableFrom(properties.getClass())) {
                                value1.remove("definitions");
                            }
                        } else {
                            deleteInnerDefinitions(value1);
                        }
                    }
                }
            }
        }
    }
}
