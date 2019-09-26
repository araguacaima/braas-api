package com.araguacaima.braas.api.model;

import com.araguacaima.commons.utils.JsonUtils;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Base64;
import java.util.Collection;
import java.util.Map;

public class BraasDrools {
    private static final JsonUtils jsonUtils = new JsonUtils();
    private static Logger log = LoggerFactory.getLogger(BraasDrools.class);
    private ObjectId id;
    private String braasId;
    //Binary spreadsheet information that contains the rule's base
    private Spreadsheet spreadsheet;
    //A json string with a map or array that contains every class schema used within rules.
    private String schemas;

    public BraasDrools() {
        this.id = new ObjectId();
    }

    public Spreadsheet getSpreadsheet() {
        return spreadsheet;
    }

    public void setSpreadsheet(Spreadsheet spreadsheet) {
        this.spreadsheet = spreadsheet;
    }

    public String getSchemas() {
        return schemas;
    }

    public void setSchemas(String schemas) {
        this.schemas = schemas;
    }

    public String getBraasId() {
        return braasId;
    }

    public void setBraasId(String braasId) {
        this.braasId = braasId;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
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
                try {
                    return jsonUtils.fromJSON(this.schemas, Collection.class);
                } catch (IOException ignored) {
                }
            } catch (Throwable t) {
                log.debug("Invalid json object as a Collection" + t.getMessage());
            }
        }
        return null;
    }

    public Map getSchemaMap() {
        if (this.schemas != null) {
            try {
                try {
                    return jsonUtils.fromJSON(this.schemas, Map.class);
                } catch (IOException ignored) {
                }
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
