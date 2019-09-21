/**
 * Copyright © 2010-2017 Nokia
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.araguacaima.braas.api.jsonschema;

import com.araguacaima.commons.utils.ReflectionUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.codemodel.*;
import org.apache.commons.lang3.StringUtils;
import org.jsonschema2pojo.GenerationConfig;
import org.jsonschema2pojo.Schema;
import org.jsonschema2pojo.rules.RuleFactory;

import java.lang.reflect.Field;
import java.util.Map;


/**
 * Applies the schema rules that represent a property definition.
 *
 * @see <a href=
 * "http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.2">http:/
 * /tools.ietf.org/html/draft-zyp-json-schema-03#section-5.2</a>
 */
public class PropertyRule extends org.jsonschema2pojo.rules.PropertyRule {

    private static final ReflectionUtils reflectionUtils = new ReflectionUtils(null);
    private final String definitionsRoot;

    PropertyRule(RuleFactory ruleFactory, String definitionsRoot) {
        super(ruleFactory);
        this.definitionsRoot = definitionsRoot;
    }

    /**
     * Applies this schema rule to take the required code generation steps.
     * <p>
     * This rule adds a property to a given Java class according to the Java
     * Bean spec. A private field is added to the class, along with accompanying
     * accessor methods.
     * <p>
     * If this rule's schema mapper is configured to include builder methods
     * (see {@link GenerationConfig#isGenerateBuilders()} ),
     * then a builder method of the form <code>withFoo(Foo foo);</code> is also
     * added.
     *
     * @param nodeName the name of the property to be applied
     * @param node     the node describing the characteristics of this property
     * @param parent   the parent node
     * @param jclass   the Java class which should have this property added
     * @return the given jclass
     */
    @Override
    public JDefinedClass apply(String nodeName, JsonNode node, JsonNode parent, JDefinedClass jclass, Schema schema) {
        super.apply(nodeName, node, parent, jclass, schema);
        JsonNode $ref = node.get("$ref");
        if ($ref != null) {
            String ref = $ref.asText();
            if (StringUtils.isNotBlank(ref)) {
                int indexOfDefinitionsRoot = ref.indexOf(definitionsRoot);
                if (indexOfDefinitionsRoot != -1) {
                    if (ref.startsWith("#")) {
                        ref = ref.substring(1);
                    }
                    if (ref.startsWith("/")) {
                        ref = ref.substring(1);
                    }
                    ref = ref.substring(definitionsRoot.length());
                    if (ref.startsWith("/")) {
                        ref = ref.substring(1);
                    }
                }
                PackageClassUtils packageClassUtils = new PackageClassUtils(ref).invoke();
                ref = packageClassUtils.getPackageName();
                String className = packageClassUtils.getClassName();
                JFieldVar fieldVar = jclass.fields().get(nodeName);
                if (fieldVar != null) {
                    JType type = fieldVar.type();
                    if (type.getClass().isAssignableFrom(JDefinedClass.class)) {
                        JDefinedClass jDefinedClass = (JDefinedClass) type;
                        JPackage jPackage = jDefinedClass._package();
                        if (!jPackage.name().equals(ref)) {
                            Field fieldOuter = reflectionUtils.getField(JDefinedClass.class, "outer");
                            try {
                                fieldOuter.setAccessible(true);
                                JClassContainer outer = (JClassContainer) fieldOuter.get(type);
                                if (outer.isPackage()) {
                                    JCodeModel owner = jclass.owner();
                                    JPackage newPackage = owner._package(ref);
                                    fieldOuter.set(type, newPackage);
                                    Field fieldClasses = reflectionUtils.getField(JPackage.class, "classes");
                                    try {
                                        fieldClasses.setAccessible(true);
                                        Map<String, JDefinedClass> classesNew = (Map<String, JDefinedClass>) fieldClasses.get(newPackage);
                                        classesNew.put(className, jDefinedClass);
                                        Map<String, JDefinedClass> classesOld = (Map<String, JDefinedClass>) fieldClasses.get(outer);
                                        classesOld.remove(className);
                                    } catch (Throwable t) {
                                        t.printStackTrace();
                                    }
                                }
                            } catch (Throwable t) {
                                t.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
        return jclass;
    }

}
