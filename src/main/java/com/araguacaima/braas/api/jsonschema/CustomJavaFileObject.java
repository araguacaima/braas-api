package com.araguacaima.braas.api.jsonschema;

import javax.tools.SimpleJavaFileObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;


public class CustomJavaFileObject extends SimpleJavaFileObject {
    private final String binaryName;
    private final URI uri;
    private final String name;
    private OutputStream outputStream;

    public CustomJavaFileObject(String binaryName, URI uri, OutputStream outputStream) {
        super(uri, Kind.CLASS);
        this.uri = uri;
        this.binaryName = binaryName;
        name = uri.getPath() == null ? uri.getSchemeSpecificPart() : uri.getPath();
        this.outputStream = outputStream;
    }

    @Override
    public URI toUri() {
        return uri;
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return uri.toURL().openStream(); // easy way to handle any URI!
    }

    @Override
    public OutputStream openOutputStream() {
        return this.outputStream;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long getLastModified() {
        return 0;
    }

    @Override
    public Kind getKind() {
        return Kind.CLASS;
    }

    @Override // copied from SImpleJavaFileManager
    public boolean isNameCompatible(String simpleName, Kind kind) {
        String baseName = simpleName + kind.extension;
        return kind.equals(getKind())
                && (baseName.equals(getName())
                || getName().endsWith("/" + baseName));
    }

    String binaryName() {
        return binaryName;
    }


    @Override
    public String toString() {
        return "CustomJavaFileObject{" +
                "uri=" + uri +
                '}';
    }
}