package com.araguacaima.braas.api;

import de.neuland.jade4j.template.ClasspathTemplateLoader;

import java.io.File;
import java.io.Reader;

/**
 * Load templates from a given directory on the classpath.
 */
public class Loader extends ClasspathTemplateLoader {

    private String templateRoot;

    /**
     * Construct a classpath loader using the given template root.
     *
     * @param templateRoot the template root directory
     */
    public Loader(String templateRoot) {
        this.templateRoot = templateRoot;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Reader getReader(String name) {
        Reader reader = null;
        if (!name.startsWith("/") && !name.startsWith("\\")) {
            name = File.separator + name;
        }
        name = name.replaceAll("\\\\", "/");
        String name1 = templateRoot + name.split("\\?")[0];
        try {
            reader = super.getReader(name1);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return reader;
    }
}
