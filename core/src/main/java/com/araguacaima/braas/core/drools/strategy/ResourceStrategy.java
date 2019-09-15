package com.araguacaima.braas.core.drools.strategy;

import java.io.ByteArrayOutputStream;

/**
 * Created by Alejandro on 12/01/2015.
 */
public interface ResourceStrategy {

    String buildUrl();

    ByteArrayOutputStream getStream();
}
