package com.araguacaima.braas.api.controller;

import com.araguacaima.braas.api.exception.InternalBraaSException;
import com.araguacaima.braas.core.drools.BraasClassLoader;
import com.araguacaima.braas.core.drools.DroolsConfig;
import com.araguacaima.commons.exception.core.PropertiesUtilException;
import com.araguacaima.commons.utils.FileUtils;
import com.araguacaima.commons.utils.JsonSchemaUtils;
import com.araguacaima.commons.utils.PropertiesHandler;
import com.araguacaima.commons.utils.StringUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.kie.api.KieBase;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.*;

import static com.araguacaima.braas.api.common.Commons.JSON_SUFFIX;

public class ApiController {

    public static ClassLoader buildClassesFromMultipartJsonSchema_(File schemaFile, String fileNameFromPart, File sourceClassesDir, File compiledClassesDir) throws InternalBraaSException {
        ClassLoader classLoader;
        try {
            classLoader = new BraasClassLoader(compiledClassesDir.toURI().toURL(), KieBase.class.getClassLoader());
            JsonSchemaUtils jsonSchemaUtils = new JsonSchemaUtils(classLoader);
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
            JsonSchemaUtils jsonSchemaUtils = new JsonSchemaUtils(new BraasClassLoader(compiledClassesDir.toURI().toURL(), KieBase.class.getClassLoader()));
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

    public static DroolsConfig createDroolsConfig(String rulesPath, ClassLoader classLoader, DroolsConfig droolsConfig) throws InternalBraaSException {
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

    public static DroolsConfig createDroolsConfig(String rulesPath, Set<Class<?>> classes, DroolsConfig droolsConfig) throws InternalBraaSException {
        try {
            if (StringUtils.isNotBlank(rulesPath)) {
                if (droolsConfig == null) {
                    Properties props = new PropertiesHandler("drools-absolute-path-decision-table.properties", ApiController.class.getClassLoader()).getProperties();
                    props.setProperty("decision.table.path", rulesPath);
                    droolsConfig = new DroolsConfig(props);
                    droolsConfig.addClasses(classes);
                }
            }
        } catch (FileNotFoundException | MalformedURLException | URISyntaxException | PropertiesUtilException e) {
            throw new InternalBraaSException(e);
        }
        return droolsConfig;
    }
}
