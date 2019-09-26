package com.araguacaima.braas.api.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.bson.types.ObjectId;

import java.io.IOException;

public class BraasDroolsDeserializer extends StdDeserializer<BraasDrools> {

    public BraasDroolsDeserializer() {
        this(null);
    }

    public BraasDroolsDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public BraasDrools deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        BraasDrools braasDrools = new BraasDrools();
        JsonNode node = jp.getCodec().readTree(jp);
        if (node != null) {
            JsonNode nodeId = node.get("id");
            if (nodeId != null) {
                ObjectId id = new ObjectId(nodeId.asText());
                braasDrools.setId(id);
            } else {
                braasDrools.setId(null);
            }
            JsonNode braasIdNode = node.get("braasId");
            if (braasIdNode != null) {
                String braasId = braasIdNode.asText();
                braasDrools.setBraasId(braasId);
            }
            JsonNode nodeSpreadsheet = node.get("spreadsheet");
            if (nodeSpreadsheet != null) {
                BraasDrools.Spreadsheet spreadsheet = new BraasDrools.Spreadsheet();
                JsonNode binaryNode = nodeSpreadsheet.get("binary");
                if (binaryNode != null) {
                    spreadsheet.setBinary(binaryNode.asText());
                }
                braasDrools.setSpreadsheet(spreadsheet);
            }
            JsonNode nodeSchemas = node.get("schemas");
            if (nodeSchemas != null) {
                braasDrools.setSchemas(nodeSchemas.toString());
            }
            return braasDrools;
        }
        return null;
    }
}
