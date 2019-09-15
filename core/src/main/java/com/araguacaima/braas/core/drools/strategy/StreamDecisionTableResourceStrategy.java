package com.araguacaima.braas.core.drools.strategy;

import java.io.ByteArrayOutputStream;

/**
 * Created by Alejandro on 12/01/2015.
 */
public class StreamDecisionTableResourceStrategy implements ResourceStrategy {

    private ByteArrayOutputStream excelStream;

    public StreamDecisionTableResourceStrategy(ByteArrayOutputStream excelStream) {
        this.excelStream = excelStream;
    }

    @Override
    public String buildUrl() {
        return null;
    }

    @Override
    public ByteArrayOutputStream getStream() {
        return this.excelStream;
    }
}
