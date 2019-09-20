package com.araguacaima.braas.api.jsonschema;

import com.araguacaima.commons.utils.FileUtils;

import javax.tools.*;
import java.io.*;
import java.net.URI;
import java.util.Collections;
import java.util.Set;

public class CustomClassloaderJavaFileManager<M extends JavaFileManager> extends ForwardingJavaFileManager<M> {
    private final ClassLoader classLoader;
    private final M standardFileManager;
    private final PackageInternalsFinder finder;
    private final File outputDirectory;


    public CustomClassloaderJavaFileManager(ClassLoader classLoader, final M fileManager, File outputDirectory) {
        super(fileManager);
        this.classLoader = classLoader;
        this.standardFileManager = fileManager;
        finder = new PackageInternalsFinder(classLoader);
        this.outputDirectory = outputDirectory;
    }

    @Override
    public ClassLoader getClassLoader(Location location) {
        return classLoader;
    }

    @Override
    public String inferBinaryName(Location location, JavaFileObject file) {
        if (file instanceof CustomJavaFileObject) {
            return ((CustomJavaFileObject) file).binaryName();
        } else { // if it's not CustomJavaFileObject, then it's coming from standard file manager - let it handle the file
            return standardFileManager.inferBinaryName(location, file);
        }
    }

    @Override
    public boolean hasLocation(Location location) {
        return location == StandardLocation.CLASS_PATH || location == StandardLocation.PLATFORM_CLASS_PATH; // we don't care about source and other location types - not needed for compilation
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) throws IOException {

        OutputStream outputStream;
        try {
            PackageClass packageClass = PackageClass.instance(className);
            String package_ = packageClass.getPackageName();
            String class_ = packageClass.getClassName();
            File outputFile = FileUtils.makeDirFromPackageName(outputDirectory, package_);
            outputFile = new File(outputFile, class_ + ".class");
            outputStream = new FileOutputStream(outputFile);
        } catch (IOException e) {
            e.printStackTrace();
            outputStream = new ByteArrayOutputStream();
        }
        URI uri = new File(className).toURI();
        return new CustomJavaFileObject(uri.getPath(), uri, outputStream);
    }

    @Override
    public Iterable<JavaFileObject> list(Location location, String packageName, Set<JavaFileObject.Kind> kinds, boolean recurse) throws IOException {
        if (location == StandardLocation.PLATFORM_CLASS_PATH) { // let standard manager hanfle
            return standardFileManager.list(location, packageName, kinds, recurse);
        } else if (location == StandardLocation.CLASS_PATH && kinds.contains(JavaFileObject.Kind.CLASS)) {
            if (packageName.startsWith("java")) { // a hack to let standard manager handle locations like "java.lang" or "java.util". Prob would make sense to join results of standard manager with those of my finder here
                return standardFileManager.list(location, packageName, kinds, recurse);
            } else { // app specific classes are here
                return finder.find(packageName);
            }
        }
        return Collections.emptyList();

    }

}