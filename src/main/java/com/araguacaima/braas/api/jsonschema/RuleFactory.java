package com.araguacaima.braas.api.jsonschema;

import com.araguacaima.commons.utils.ReflectionUtils;
import com.sun.codemodel.JDefinedClass;
import org.jsonschema2pojo.Annotator;
import org.jsonschema2pojo.GenerationConfig;
import org.jsonschema2pojo.SchemaStore;
import org.jsonschema2pojo.rules.Rule;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class RuleFactory extends org.jsonschema2pojo.rules.RuleFactory {

    private static final ReflectionUtils reflectionUtils = new ReflectionUtils(null);
    private final String definitionsRoot;

    public RuleFactory(GenerationConfig generationConfig, Annotator annotator, SchemaStore schemaStore, String definitionsRoot) throws NoSuchFieldException, IllegalAccessException {
        super(generationConfig, annotator, schemaStore);
        this.definitionsRoot = definitionsRoot;
        Field field = org.jsonschema2pojo.rules.RuleFactory.class.getDeclaredField("nameHelper");
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        int modifiers = modifiersField.getInt(field);

        // blank out the final bit in the modifiers int
        modifiers &= ~Modifier.FINAL;
        modifiersField.setInt(field, modifiers);
        field.set(this, new com.araguacaima.braas.api.jsonschema.NameHelper(generationConfig, true));
    }

    /**
     * Provides a rule instance that should be applied when a property
     * declaration (child of the "properties" declaration) is found in the
     * schema.
     *
     * @return a schema rule that can handle a property declaration.
     */
    @Override
    public Rule<JDefinedClass, JDefinedClass> getPropertyRule() {
        return new PropertyRule(this, definitionsRoot);
    }


}
