package com.araguacaima.braas.api.jsonschema;

import javax.tools.SimpleJavaFileObject;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;

public class JavaFileObject extends SimpleJavaFileObject {
    private final ByteArrayOutputStream os = new ByteArrayOutputStream();

    JavaFileObject(String name, Kind kind) {
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
