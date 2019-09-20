package com.araguacaima.braas.api.jsonschema;

import com.araguacaima.commons.utils.StringUtils;

public class PackageClass {
    private String id;
    private String className;
    private String packageName;

    public PackageClass(String id) {
        this.id = id;
        if (id != null) {
            this.id = id.replaceAll("/", ".").replaceAll("\\\\", ".");
        }
    }

    public static PackageClass instance(String id) {
        return new PackageClass(id).invoke();
    }

    public String getClassName() {
        return className;
    }

    public String getPackageName() {
        return packageName;
    }

    public PackageClass invoke() {
        if (id.contains(".")) {
            className = id.substring(id.lastIndexOf('.') + 1);
            packageName = id.substring(0, id.lastIndexOf('.'));
        } else {
            className = id;
            packageName = StringUtils.EMPTY;
        }
        return this;
    }
}
