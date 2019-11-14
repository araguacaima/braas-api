package com.araguacaima.braas.api.controller.jsonschema;

import com.sun.codemodel.JCodeModel;
import org.jsonschema2pojo.SchemaMapper;

import java.io.File;
import java.io.IOException;

public class JsonUtils {

    public void jsonToSourceClassFile(String json, String className, String packageName, File rootDirectory, RuleFactory ruleFactory, org.jsonschema2pojo.SchemaGenerator schemaGenerator) throws IOException {
        JCodeModel codeModel = new JCodeModel();
        SchemaMapper mapper = new SchemaMapper(ruleFactory, schemaGenerator);
        mapper.generate(codeModel, className, packageName, json);
        codeModel.build(rootDirectory);
    }

}
