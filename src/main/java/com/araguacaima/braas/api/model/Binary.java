package com.araguacaima.braas.api.model;

import java.util.Collection;
import java.util.Map;

public class Binary {

    //Binary spreadsheet information that contains the rule's base
    private String binary;
    //A json object with a map or array that contains every class schema used within rules.
    private String schema;

    private byte[] binary_;

    private Collection schemaArray;

    private Map schemaMap;

    public String getBinary() {
        return binary;
    }

    public void setBinary(String binary) {
        this.binary = binary;
        //TODO Convert to a byte[]: binary_
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
        //TODO Convert to a Collection schemaArray or Map schemaMap
    }
}
