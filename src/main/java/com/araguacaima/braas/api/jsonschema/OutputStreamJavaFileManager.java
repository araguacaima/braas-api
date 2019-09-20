package com.araguacaima.braas.api.jsonschema;

import com.araguacaima.commons.utils.FileUtils;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import java.io.*;

public class OutputStreamJavaFileManager<M extends JavaFileManager>
        extends ForwardingJavaFileManager<M> {
    private File outputDirectory;

    public OutputStreamJavaFileManager(final M fileManager, final File outputDirectory) {
        super(fileManager);
        this.outputDirectory = outputDirectory;
    }

    @Override
    public JavaFileObject getJavaFileForOutput(final Location location,
                                               final String className, final JavaFileObject.Kind kind, final FileObject sibling) {
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
        return new OutputStreamSimpleFileObject(new File(className).toURI(), kind, outputStream);
    }
}
