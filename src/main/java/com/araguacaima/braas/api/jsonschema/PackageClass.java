package com.araguacaima.braas.api.jsonschema;

import com.araguacaima.commons.utils.FileUtils;
import com.araguacaima.commons.utils.StringUtils;

import java.io.File;

public class PackageClass {

    private static FileUtils fileUtils = new FileUtils();


    private String id;
    private String className;
    private String packageName;
    private String fullyQualifiedClassName;


    public PackageClass(String id) {
        this.id = id;
        if (this.id != null) {
            if (this.id.startsWith(".") || this.id.startsWith("\\") || this.id.startsWith("/")) {
                this.id = this.id.substring(1);
            }
            if (this.id.endsWith(".java") || this.id.endsWith(".class")) {
                this.id = this.id.substring(0, this.id.length() - 5);
            }

            this.id = this.id.replaceAll("/", ".").replaceAll("\\\\", ".");
        }
    }

    public static PackageClass instance(String id) {
        return new PackageClass(id).invoke();
    }

    public static PackageClass instance(File root, File relative, String suffix) {
        return instance(fileUtils.getRelativePathFrom(root, relative).substring(1) + File.separator + relative.getName().replace(suffix, StringUtils.EMPTY));
    }

    public String getClassName() {
        return className;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getFullyQualifiedClassName() {
        return fullyQualifiedClassName;
    }

    public PackageClass invoke() {
        if (id.contains(".")) {
            className = id.substring(id.lastIndexOf('.') + 1);
            packageName = id.substring(0, id.lastIndexOf('.'));
        } else {
            className = id;
            packageName = StringUtils.EMPTY;
        }
        fullyQualifiedClassName = packageName + "." + className;
        return this;
    }
}
