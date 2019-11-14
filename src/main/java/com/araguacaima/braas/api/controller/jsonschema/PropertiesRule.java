package com.araguacaima.braas.api.controller.jsonschema;

import com.araguacaima.commons.utils.jsonschema.RuleFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.sun.codemodel.*;
import org.jsonschema2pojo.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

public class PropertiesRule extends org.jsonschema2pojo.rules.PropertiesRule {

    private static final Logger logger = LoggerFactory.getLogger(com.araguacaima.commons.utils.jsonschema.PropertiesRule.class);
    private final RuleFactory ruleFactory;

    PropertiesRule(RuleFactory ruleFactory) {
        super(ruleFactory);
        this.ruleFactory = ruleFactory;
    }

    /**
     * Applies this schema rule to take the required code generation steps.
     * <p>
     * For each property present within the properties node, this rule will
     * invoke the 'property' rule provided by the given schema mapper.
     *
     * @param nodeName the name of the node for which properties are being added
     * @param node     the properties node, containing property names and their
     *                 definition
     * @param jclass   the Java type which will have the given properties added
     * @return the given jclass
     */
    @Override
    public JDefinedClass apply(String nodeName, JsonNode node, JsonNode parent, JDefinedClass jclass, Schema schema) {
        if (node == null) {
            node = JsonNodeFactory.instance.objectNode();
        }

        for (Iterator<String> properties = node.fieldNames(); properties.hasNext(); ) {
            String property = properties.next();
            JsonNode node1 = node.get(property);
            JDefinedClass type = ruleFactory.getPropertyRule().apply(property, node1, node, jclass, schema);
            if (isComplexType(node1)) {
                JDefinedClass type1 = (JDefinedClass) jclass.fields().get(property).type().boxify();
                String generatedClassName = type1.getPackage().name() + "." + type1.name();
                ruleFactory.addGeneratedType(generatedClassName, type1);
            }
        }

        if (ruleFactory.getGenerationConfig().isGenerateBuilders() && !jclass._extends().name().equals("Object")) {
            addOverrideBuilders(jclass, jclass.owner()._getClass(jclass._extends().fullName()));
        }

        ruleFactory.getAnnotator().propertyOrder(jclass, node);

        return jclass;
    }

    private boolean isComplexType(JsonNode node) {
        JsonNode $ref = node.get("$ref");
        return $ref != null;
    }

    private void addOverrideBuilders(JDefinedClass jclass, JDefinedClass parentJclass) {
        if (parentJclass == null) {
            return;
        }

        for (JMethod parentJMethod : parentJclass.methods()) {
            if (parentJMethod.name().startsWith("with") && parentJMethod.params().size() == 1) {
                addOverrideBuilder(jclass, parentJMethod, parentJMethod.params().get(0));
            }
        }
    }

    private void addOverrideBuilder(JDefinedClass thisJDefinedClass, JMethod parentBuilder, JVar parentParam) {

        // Confirm that this class doesn't already have a builder method matching the same name as the parentBuilder
        if (thisJDefinedClass.getMethod(parentBuilder.name(), new JType[]{parentParam.type()}) == null) {

            JMethod builder = thisJDefinedClass.method(parentBuilder.mods().getValue(), thisJDefinedClass, parentBuilder.name());
            builder.annotate(Override.class);

            JVar param = builder.param(parentParam.type(), parentParam.name());
            JBlock body = builder.body();
            body.invoke(JExpr._super(), parentBuilder).arg(param);
            body._return(JExpr._this());

        }
    }

}
