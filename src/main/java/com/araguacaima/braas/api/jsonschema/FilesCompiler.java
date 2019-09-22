package com.araguacaima.braas.api.jsonschema;

import com.araguacaima.braas.core.drools.ReloadableClassLoader;
import org.joor.Reflect;
import org.joor.ReflectException;

import javax.tools.*;
import java.io.*;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.*;

@SuppressWarnings("unused")
public class FilesCompiler {

    private static final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    private ClassLoader classLoader;

    public FilesCompiler() {
        this(MethodHandles.lookup().lookupClass().getClassLoader());
    }

    public FilesCompiler(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public Set<Class<?>> compile(List<String> options, File sourceCodeDirectory, Collection<File> files) throws IOException {
        List<CharSequenceJavaFileObject> files_ = new ArrayList<>();
        for (File file : files) {
            PackageClassUtils packageClassUtils = PackageClassUtils.instance(sourceCodeDirectory, file, ".java");
            String content = org.apache.commons.io.FileUtils.readFileToString(file, Charset.forName("UTF-8"));
            String className = packageClassUtils.getFullyQualifiedClassName();
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
                    String err = String.format("Compilation error: Line %d - %s%n", d.getLineNumber(), d.getMessage(null));
                    System.err.print(err);
                }
            }

            if (fileManager.isEmpty()) {
                throw new ReflectException("Compilation error: " + out);
            }

            Set<Class<?>> resultList = new LinkedHashSet<>();
            for (CharSequenceJavaFileObject file : files) {
                String className = PackageClassUtils.instance(file.getName()).getFullyQualifiedClassName();
                Class<?> result;
                try {
                    classLoader.loadClass(className);
                    ReloadableClassLoader tempClassLoader = new ReloadableClassLoader(classLoader);
                    result = fileManager.loadAndReturnMainClass(className, (name, bytes) -> {
                        Reflect.on(tempClassLoader).call("defineClass", name, bytes, 0, bytes.length).get();
                        return tempClassLoader.loadClass(name, bytes);
                    });
                    classLoader = tempClassLoader;
                } catch (Throwable ignored) {
                    result = fileManager.loadAndReturnMainClass(className,
                            (name, bytes) -> Reflect.on(classLoader).call("defineClass", name, bytes, 0, bytes.length).get());
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

    public Set<Class<?>> compile(File sourceCodeDirectory, File compiledClassesDirectory, Collection<File> listFiles) throws IOException, URISyntaxException {
        List<String> options = Arrays.asList("-d", compiledClassesDirectory.getCanonicalPath());
        if (!options.contains("-classpath")) {
            StringBuilder classpath = new StringBuilder();
            String separator = System.getProperty("path.separator");
            String prop = System.getProperty("java.class.path");
            if (prop != null && !"".equals(prop)) {
                classpath.append(prop);
            }
            if (classLoader instanceof URLClassLoader) {
                for (URL url : ((URLClassLoader) classLoader).getURLs()) {
                    if (classpath.length() > 0) {
                        classpath.append(separator);
                    }
                    if ("file".equals(url.getProtocol())) {
                        classpath.append(new File(url.toURI()));
                    }
                }
            }
            options.addAll(Arrays.asList("-classpath", classpath.toString()));
        }
        return compile(options, sourceCodeDirectory, listFiles);
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    @FunctionalInterface
    interface ThrowingBiFunction<T, U, R> {
        R apply(T t, U u) throws Exception;
    }

    private static final class JavaFileObject extends SimpleJavaFileObject {
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

    private static final class ClassFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {
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
                FileObject sibling) {
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

                for (Map.Entry<String, FilesCompiler.JavaFileObject> entry : fileObjectMap.entrySet()) {
                    classes.put(entry.getKey(), entry.getValue().getBytes());
                }
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

    private static final class CharSequenceJavaFileObject extends SimpleJavaFileObject {
        final CharSequence content;

        CharSequenceJavaFileObject(String className, CharSequence content) {
            super(URI.create("string:///" + className.replace('.', '/') + FilesCompiler.JavaFileObject.Kind.SOURCE.extension), FilesCompiler.JavaFileObject.Kind.SOURCE);
            this.content = content;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return content;
        }
    }

}


