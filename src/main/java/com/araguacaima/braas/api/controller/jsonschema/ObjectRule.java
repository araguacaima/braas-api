package com.araguacaima.braas.api.controller.jsonschema;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.JType;
import org.jsonschema2pojo.Schema;
import org.jsonschema2pojo.rules.Rule;
import org.jsonschema2pojo.util.ParcelableHelper;
import org.jsonschema2pojo.util.ReflectionHelper;

import java.util.Map;

public class ObjectRule extends org.jsonschema2pojo.rules.ObjectRule {

    private final RuleFactory ruleFactory;

    protected ObjectRule(RuleFactory ruleFactory, ParcelableHelper parcelableHelper, ReflectionHelper reflectionHelper) {
        super(ruleFactory, parcelableHelper, reflectionHelper);
        this.ruleFactory = ruleFactory;
    }

    /**
     * Applies this schema rule to take the required code generation steps.
     * <p>
     * When this rule is applied for schemas of type object, the properties of
     * the schema are used to generate a new Java class and determine its
     * characteristics. See other implementers of {@link Rule} for details.
     */
    @Override
    public JType apply(String nodeName, JsonNode node, JsonNode parent, JPackage _package, Schema schema) {
        Map<String, JType> generatedTypes = ruleFactory.getGeneratedTypes();
        JType jType = generatedTypes.get(nodeName);
        String fullClassName = _package.name() + "." + nodeName;
        if (jType == null) {
            jType = ruleFactory.getGeneratedType(fullClassName);
        }
        if (jType == null) {
            jType = super.apply(nodeName, node, parent, _package, schema);
            generatedTypes.put(fullClassName, jType);
        }
        return jType;
    }
}
