package com.araguacaima.braas.api.model;

import java.util.Base64;
import java.util.Collection;
import java.util.Map;

public class Binary {

    //Binary spreadsheet information that contains the rule's base
    private Spreadsheet spreadsheet;
    //A json object with a map or array that contains every class schema used within rules.
    private String schema;

    private Collection schemaArray;

    private Map schemaMap;

    public Spreadsheet getSpreadsheet() {
        return spreadsheet;
    }

    public void setSpreadsheet(Spreadsheet spreadsheet) {
        this.spreadsheet = spreadsheet;
        //TODO Convert to a byte[]: binary_
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
        //TODO Convert to a Collection schemaArray or Map schemaMap
    }

    public byte[] getBinary_() throws IllegalArgumentException {
        if (this.spreadsheet != null && this.spreadsheet.binary != null) {
            String binary = this.spreadsheet.binary;
            return Base64.getDecoder().decode(binary);
        }
        return null;
    }

    public Collection getSchemaArray() {
        return schemaArray;
    }

    public Map getSchemaMap() {
        return schemaMap;
    }

    public static class Spreadsheet {
        //Base64 binary spreadsheet bytes array information that contains the rule's base
        private String binary;

        public String getBinary() {
            return binary;
        }

        public void setBinary(String binary) {
            this.binary = binary;
        }
    }
}
