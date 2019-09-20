package com.araguacaima.braas.api.jsonschema;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.StandardJavaFileManager;

public class ClassFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {
    private JavaFileObject object;

    ClassFileManager(StandardJavaFileManager m) {
        super(m);
    }

    @Override
    public JavaFileObject getJavaFileForOutput(
            JavaFileManager.Location location,
            String className,
            JavaFileObject.Kind kind,
            FileObject sibling) {
        return object = new JavaFileObject(className, kind);
    }

    public JavaFileObject getObject() {
        return object;
    }
}