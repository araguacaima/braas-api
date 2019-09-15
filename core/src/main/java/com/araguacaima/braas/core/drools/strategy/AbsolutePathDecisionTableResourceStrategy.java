package com.araguacaima.braas.core.drools.strategy;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * Created by Alejandro on 12/01/2015.
 */
public class AbsolutePathDecisionTableResourceStrategy implements ResourceStrategy {

    String excelFileName;

    public AbsolutePathDecisionTableResourceStrategy(File excelFile)
            throws IOException {
        this.excelFileName = excelFile.getCanonicalPath();
    }

    @Override
    public String buildUrl() {
        return excelFileName;
    }

    @Override
    public ByteArrayOutputStream getStream() {
        return null;
    }
}
