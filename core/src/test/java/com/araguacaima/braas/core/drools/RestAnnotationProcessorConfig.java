package com.araguacaima.braas.core.drools;

import com.araguacaima.commons.utils.ClassLoaderUtils;
import com.araguacaima.commons.utils.JsonUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.lang.reflect.Method;
import java.util.*;


public class RestAnnotationProcessorConfig {
    
    private Map<Pair<Object, String>,
            Map<Object, Map<Object, Set<File>>>> resourcesMap = new HashMap<>();
    protected Method method;
    protected Collection<String> fullArtifactsPath;
    private String rootPath;
    private String includeCommonTypes;
    private boolean alwaysExpandsParentAttributes;
    private boolean jaxService;
    private Class mainClass;
    protected Collection<Class> alreadyProcessedTypes = new ArrayList<>();
    protected Collection<Class> alreadyProcessedExamples = new ArrayList<>();
    private boolean paginatedResource = false;
    private Map<String, String> paramUUID = new HashMap<>();
    private boolean collection = false;
    private boolean splitEndpoints;
    private boolean flattenJoinedEntitiesTypes;
    private boolean functional;
    private Collection<Object> objectCollection;
    private String outputPath;
    private String fullClassName;


    public void init(
            String servicePath,
            Method method,
            Collection<String> fullArtifactsPath) {
        this.rootPath = servicePath;
        this.method = method;
        this.fullArtifactsPath = fullArtifactsPath;
    }

    public Map<Pair<Object, String>, Map<Object, Map<Object, Set<File>>>> getResourcesMap() {
        return resourcesMap;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Method getMethod() {
        return method;
    }

    public void setFullArtifactsPath(Collection<String> fullArtifactsPath) {
        this.fullArtifactsPath = fullArtifactsPath;
    }

    public Collection<String> getFullArtifactsPath() {
        return fullArtifactsPath;
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public String getIncludeCommonTypes() {
        return includeCommonTypes;
    }

    public void setIncludeCommonTypes(String includeCommonTypes) {
        this.includeCommonTypes = includeCommonTypes;
    }

    public boolean isAlwaysExpandsParentAttributes() {
        return alwaysExpandsParentAttributes;
    }

    public void setAlwaysExpandsParentAttributes(boolean alwaysExpandsParentAttributes) {
        this.alwaysExpandsParentAttributes = alwaysExpandsParentAttributes;
    }

    public boolean isJaxService() {
        return this.jaxService;
    }

    public void setJaxService(boolean jaxService) {
        this.jaxService = jaxService;
    }

    public Class getMainClass() {
        return mainClass;
    }

    public void setMainClass(Class mainClass) {
        this.mainClass = mainClass;
    }

    public Collection<Class> getAlreadyProcessedTypes() {
        return alreadyProcessedTypes;
    }

    public void setAlreadyProcessedTypes(Collection<Class> alreadyProcessedTypes) {
        this.alreadyProcessedTypes = alreadyProcessedTypes;
    }

    public void addProcessedType(Class processedType) {
        this.alreadyProcessedTypes.add(processedType);
    }

    public Collection<Class> getAlreadyProcessedExamples() {
        return alreadyProcessedExamples;
    }

    public void setAlreadyProcessedExamples(Collection<Class> alreadyProcessedExamples) {
        this.alreadyProcessedExamples = alreadyProcessedExamples;
    }

    public void addProcessedExample(Class processedExample) {
        this.alreadyProcessedExamples.add(processedExample);
    }

    public boolean isPaginatedResource() {
        return paginatedResource;
    }

    public void setPaginatedResource(boolean paginatedResource) {
        this.paginatedResource = paginatedResource;
    }

    public Map<String, String> getParamUUID() {
        return paramUUID;
    }

    public void setParamUUID(Map<String, String> paramUUID) {
        this.paramUUID = paramUUID;
    }

    public void addParamUUID(String param, String uuid) {
        this.paramUUID.put(param, uuid);
    }

    public boolean isCollection() {
        return collection;
    }

    public void setCollection(boolean collection) {
        this.collection = collection;
    }

    public boolean isSplitEndpoints() {
        return splitEndpoints;
    }

    public void setSplitEndpoints(boolean splitEndpoints) {
        this.splitEndpoints = splitEndpoints;
    }

    public boolean isFlattenJoinedEntitiesTypes() {
        return flattenJoinedEntitiesTypes;
    }

    public void setFlattenJoinedEntitiesTypes(boolean flattenJoinedEntitiesTypes) {
        this.flattenJoinedEntitiesTypes = flattenJoinedEntitiesTypes;
    }
    
    public boolean isFunctional() {
        return this.functional;
    }

    public void setFunctional(boolean functional) {
        this.functional = functional;
    }

    public Collection<Object> getObjectCollection() {
        return objectCollection;
    }

    public void setObjectCollection(Collection<Object> objectCollection) {
        this.objectCollection = objectCollection;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public String getFullClassName() {
        return fullClassName;
    }

    public void setFullClassName(String fullClassName) {
        this.fullClassName = fullClassName;
    }
}
