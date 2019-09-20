package com.araguacaima.braas.api.jsonschema;

import org.joor.Reflect;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.util.ArrayList;
import java.util.List;

public class Compiler {
    public static Class<?> compile(String className, String content) {
        Lookup lookup = MethodHandles.lookup();
        // If we have already compiled our class, simply load it
        try {
            return lookup.lookupClass().getClassLoader().loadClass(className);
        }
        // Otherwise, let's try to compile it
        catch (ClassNotFoundException ignore) {
            return compile0(className, content, lookup);
        }
    }

    private static Class<?> compile0(String className, String content, Lookup lookup) {
        JavaCompiler compiler =
                ToolProvider.getSystemJavaCompiler();
        ClassFileManager manager = new ClassFileManager(
                compiler.getStandardFileManager(null, null, null));
        List<CharSequenceJavaFileObject> files = new ArrayList<>();
        files.add(new CharSequenceJavaFileObject(className, content));
        compiler.getTask(null, manager, null, null, null, files).call();
        Class<?> result;
        // Implement a check whether we're on JDK 8. If so, use
        // protected ClassLoader API, reflectively

        ClassLoader cl = lookup.lookupClass().getClassLoader();
        byte[] b = manager.getObject().getBytes();
        result = Reflect.on(cl).call("defineClass",
                className, b, 0, b.length).get();

        // Lookup.defineClass() has only been introduced in Java 9.
        // It is required to get private-access to interfaces in
        // the class hierarchy

        return result;
    }
}