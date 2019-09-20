package com.araguacaima.braas.api.jsonschema;

import javax.tools.SimpleJavaFileObject;
import java.io.OutputStream;
import java.net.URI;

public class OutputStreamSimpleFileObject extends SimpleJavaFileObject {
    private OutputStream outputStream;

    public OutputStreamSimpleFileObject(final URI uri, final Kind kind,
                                        final OutputStream outputStream) {
        super(uri, kind);
        this.outputStream = outputStream;
    }

    @Override
    public OutputStream openOutputStream() {
        return this.outputStream;
    }
}
