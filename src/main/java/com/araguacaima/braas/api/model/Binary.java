package com.araguacaima.braas.api.model;

import com.araguacaima.commons.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Collection;
import java.util.Map;

public class Binary {

    private static final JsonUtils jsonUtils = new JsonUtils();
    private static Logger log = LoggerFactory.getLogger(Binary.class);

    //Binary spreadsheet information that contains the rule's base
    private Spreadsheet spreadsheet;
    //A json string with a map or array that contains every class schema used within rules.
    private Object schemas;

    public Spreadsheet getSpreadsheet() {
        return spreadsheet;
    }

    public void setSpreadsheet(Spreadsheet spreadsheet) {
        this.spreadsheet = spreadsheet;
    }

    public Object getSchemas() {
        return schemas;
    }

    public void setSchemas(Object schemas) {
        this.schemas = schemas;
    }

    public byte[] getBinary_() throws IllegalArgumentException {
        if (this.spreadsheet != null && this.spreadsheet.binary != null) {
            String binary = this.spreadsheet.binary;
            return Base64.getDecoder().decode(binary);
        }
        return null;
    }

    public Collection getSchemaArray() {
        if (this.schemas != null) {
            try {
                return (Collection) this.schemas;
            } catch (Throwable t) {
                log.debug("Invalid json object as a Collection" + t.getMessage());
            }
        }
        return null;
    }

    public Map getSchemaMap() {
        if (this.schemas != null) {
            try {
                return (Map) this.schemas;
            } catch (Throwable t) {
                log.debug("Invalid json object as a Map" + t.getMessage());
            }
        }
        return null;
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
