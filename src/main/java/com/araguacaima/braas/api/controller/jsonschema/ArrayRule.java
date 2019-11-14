package com.araguacaima.braas.api.controller.jsonschema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassContainer;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.JType;
import org.jsonschema2pojo.Schema;
import org.jsonschema2pojo.rules.Rule;
import org.jsonschema2pojo.rules.RuleFactory;
import org.jsonschema2pojo.util.Inflector;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ArrayRule extends org.jsonschema2pojo.rules.ArrayRule {

    private String definitionsRoot;
    private Map definitions;
    private RuleFactory ruleFactory;

    ArrayRule(RuleFactory ruleFactory) {
        super(ruleFactory);
    }

    ArrayRule(RuleFactory ruleFactory, String definitionsRoot, Map definitions) {
        super(ruleFactory);
        this.ruleFactory = ruleFactory;
        this.definitionsRoot = definitionsRoot;
        this.definitions = definitions;
    }

    @Override
    public JClass apply(String nodeName, JsonNode node, JsonNode parent, JPackage jpackage, Schema schema) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootDefinitions = mapper.valueToTree(definitions);
        ObjectNode schemaContent = (ObjectNode) schema.getContent();
        schemaContent.set(definitionsRoot, rootDefinitions);
        super.apply(nodeName, node, parent, jpackage, schema);
        JClass arrayType;

        boolean uniqueItems = node.has("uniqueItems") && node.get("uniqueItems").asBoolean();
        boolean rootSchemaIsArray = !schema.isGenerated();

        JType itemType;
        if (node.has("items")) {
            Rule<JClassContainer, JType> schemaRule = ruleFactory.getSchemaRule();

            JsonNode items = node.get("items");
            itemType = schemaRule.apply(nodeName, items, node, jpackage, schema);
        } else {
            itemType = jpackage.owner().ref(Object.class);
        }

        if (uniqueItems) {
            arrayType = jpackage.owner().ref(Set.class).narrow(itemType);
        } else {
            arrayType = jpackage.owner().ref(List.class).narrow(itemType);
        }

        if (rootSchemaIsArray) {
            schema.setJavaType(arrayType);
        }
        return arrayType;
    }

    private String makeSingular(String nodeName) {
        return Inflector.getInstance().singularize(nodeName);
    }
}
