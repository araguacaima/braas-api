package com.araguacaima.braas.api.jsonschema;

import com.araguacaima.commons.utils.ClassLoaderUtils;
import org.joor.Reflect;
import org.joor.ReflectException;

import javax.tools.*;
import java.io.*;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.*;

public class FilesCompiler {

    private static final MethodHandles.Lookup lookup = MethodHandles.lookup();
    private static final ClassLoader cl = lookup.lookupClass().getClassLoader();
    private static final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    private final ClassLoaderUtils classLoaderUtils;

    public FilesCompiler(ClassLoaderUtils classLoaderUtils) {
        this.classLoaderUtils = classLoaderUtils;
    }

    public Set<Class<?>> compile(List<String> options, File sourceCodeDirectory, Collection<File> files) throws IOException {
        List<CharSequenceJavaFileObject> files_ = new ArrayList<>();
        for (File file : files) {
            PackageClass packageClass = PackageClass.instance(sourceCodeDirectory, file, ".java");
            String content = org.apache.commons.io.FileUtils.readFileToString(file, Charset.forName("UTF-8"));
            String className = packageClass.getFullyQualifiedClassName();
            files_.add(new FilesCompiler.CharSequenceJavaFileObject(className, content));
        }
        return compile(options, files_);
    }

    public Set<Class<?>> compile(List<String> options, List<FilesCompiler.CharSequenceJavaFileObject> files) {

        try {
            FilesCompiler.ClassFileManager fileManager = new FilesCompiler.ClassFileManager(compiler.getStandardFileManager(null, null, null));
            StringWriter out = new StringWriter();
            DiagnosticCollector<javax.tools.JavaFileObject> diagnostics = new DiagnosticCollector<>();
            JavaCompiler.CompilationTask task = compiler.getTask(out, fileManager, diagnostics, options, null, files);

            if (!task.call()) {
                for (Diagnostic d : diagnostics.getDiagnostics()) {
                    String err = String.format("Compilation error: Line %d - %s%n", d.getLineNumber(),
                            d.getMessage(null));
                    System.err.print(err);
                }
            }

            if (fileManager.isEmpty()) {
                throw new ReflectException("Compilation error: " + out);
            }

            Set<Class<?>> resultList = new LinkedHashSet<>();
            for (CharSequenceJavaFileObject file : files) {
                String className = PackageClass.instance(file.getName()).getFullyQualifiedClassName();
                Class<?> result;
                try {
                    cl.loadClass(className);
                    ReloadableClassLoader newCl = new ReloadableClassLoader(cl);
                    result = fileManager.loadAndReturnMainClass(className,
                            (name, bytes) -> Reflect.on(newCl).call("defineClass", name, bytes, 0, bytes.length).get());
                } catch (Throwable ignored) {
                    result = fileManager.loadAndReturnMainClass(className,
                            (name, bytes) -> Reflect.on(cl).call("defineClass", name, bytes, 0, bytes.length).get());
                }
                resultList.add(result);
            }
            return resultList;
        } catch (ReflectException e) {
            throw e;
        } catch (Exception e) {
            throw new ReflectException("Error while compiling classes: " + files, e);
        }
    }

    public Set<Class<?>> compile(File sourceCodeDirectory, File compiledClassesDirectory, Collection<File> listFiles) throws IOException {
        classLoaderUtils.addToClasspath(sourceCodeDirectory.getCanonicalPath());
        List<String> options = Arrays.asList("-classpath", "\"" + classLoaderUtils.getClasspath() + "\"", "-d", compiledClassesDirectory.getCanonicalPath());
        return compile(options, sourceCodeDirectory, listFiles);
    }

    @FunctionalInterface
    interface ThrowingBiFunction<T, U, R> {
        R apply(T t, U u) throws Exception;
    }

    static final class JavaFileObject extends SimpleJavaFileObject {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();

        JavaFileObject(String name, FilesCompiler.JavaFileObject.Kind kind) {
            super(URI.create("string:///" + name.replace('.', '/') + kind.extension), kind);
        }

        byte[] getBytes() {
            return os.toByteArray();
        }

        @Override
        public OutputStream openOutputStream() {
            return os;
        }
    }

    static final class ClassFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {
        private final Map<String, FilesCompiler.JavaFileObject> fileObjectMap;
        private Map<String, byte[]> classes;

        ClassFileManager(StandardJavaFileManager standardManager) {
            super(standardManager);

            fileObjectMap = new HashMap<>();
        }

        @Override
        public FilesCompiler.JavaFileObject getJavaFileForOutput(
                JavaFileManager.Location location,
                String className,
                FilesCompiler.JavaFileObject.Kind kind,
                FileObject sibling
        ) {
            FilesCompiler.JavaFileObject result = new FilesCompiler.JavaFileObject(className, kind);
            fileObjectMap.put(className, result);
            return result;
        }

        boolean isEmpty() {
            return fileObjectMap.isEmpty();
        }

        Map<String, byte[]> classes() {
            if (classes == null) {
                classes = new HashMap<>();

                for (Map.Entry<String, FilesCompiler.JavaFileObject> entry : fileObjectMap.entrySet())
                    classes.put(entry.getKey(), entry.getValue().getBytes());
            }

            return classes;
        }

        Class<?> loadAndReturnMainClass(String mainClassName, FilesCompiler.ThrowingBiFunction<String, byte[], Class<?>> definer) throws Exception {
            Class<?> result = null;

            for (Map.Entry<String, byte[]> entry : classes().entrySet()) {
                Class<?> c = definer.apply(entry.getKey(), entry.getValue());
                if (mainClassName.equals(entry.getKey()))
                    result = c;
            }

            return result;
        }
    }

    public static final class CharSequenceJavaFileObject extends SimpleJavaFileObject {
        final CharSequence content;

        public CharSequenceJavaFileObject(String className, CharSequence content) {
            super(URI.create("string:///" + className.replace('.', '/') + FilesCompiler.JavaFileObject.Kind.SOURCE.extension), FilesCompiler.JavaFileObject.Kind.SOURCE);
            this.content = content;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return content;
        }
    }

    final class ReloadableClassLoader extends ClassLoader {

        public ReloadableClassLoader(ClassLoader parent) {
            super(parent);
        }

        public Class loadClass(String name, URL myUrl) {

            try {
                URLConnection connection = myUrl.openConnection();
                InputStream input = connection.getInputStream();
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int data = input.read();

                while (data != -1) {
                    buffer.write(data);
                    data = input.read();
                }

                input.close();

                byte[] classData = buffer.toByteArray();

                return defineClass(name, classData, 0, classData.length);

            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}


