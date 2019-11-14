/*
  Copyright © 2010-2017 Nokia
  <p>
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  <p>
  http://www.apache.org/licenses/LICENSE-2.0
  <p>
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

package com.araguacaima.braas.api.controller.jsonschema;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.codemodel.JPackage;
import org.apache.commons.lang3.text.WordUtils;
import org.jsonschema2pojo.GenerationConfig;

import static java.lang.Character.isDigit;
import static org.apache.commons.lang3.StringUtils.*;

public class NameHelper extends org.jsonschema2pojo.util.NameHelper {

    private final boolean keepClassesNamesUnaltered;
    private final GenerationConfig generationConfig;

    public NameHelper(GenerationConfig generationConfig, boolean keepClassesNamesUnaltered) {
        super(generationConfig);
        this.generationConfig = generationConfig;
        this.keepClassesNamesUnaltered = keepClassesNamesUnaltered;
    }

    @Override
    public String getClassName(String nodeName, JsonNode node, JPackage _package) {
        String prefix = generationConfig.getClassNamePrefix();
        String suffix = generationConfig.getClassNameSuffix();
        String fieldName = getClassName(nodeName, node);
        String capitalizedFieldName = capitalize(fieldName);
        String fullFieldName = createFullFieldName(capitalizedFieldName, prefix, suffix);

        String className = replaceIllegalCharacters(fullFieldName);
        return normalizeName(className);
    }

    private String createFullFieldName(String nodeName, String prefix, String suffix) {
        String returnString = nodeName;
        if (prefix != null) {
            returnString = prefix + returnString;
        }

        if (suffix != null) {
            returnString = returnString + suffix;
        }

        return returnString;
    }

    @Override
    public String normalizeName(String name) {
        name = capitalizeTrailingWords(name);

        if (isDigit(name.charAt(0))) {
            name = "_" + name;
        }

        return name;
    }

    @Override
    public String capitalizeTrailingWords(String name) {
        char[] wordDelimiters = generationConfig.getPropertyWordDelimiters();

        if (containsAny(name, wordDelimiters)) {
            String capitalizedNodeName;
            if (areAllWordsUpperCaseBesideDelimiters(name, wordDelimiters)) {
                if (!keepClassesNamesUnaltered) {
                    capitalizedNodeName = WordUtils.capitalizeFully(name, wordDelimiters);
                } else {
                    capitalizedNodeName = name;
                }
            } else {
                if (!keepClassesNamesUnaltered) {
                    capitalizedNodeName = WordUtils.capitalize(name, wordDelimiters);
                } else {
                    capitalizedNodeName = name;
                }
            }
            name = name.charAt(0) + capitalizedNodeName.substring(1);

            for (char c : wordDelimiters) {
                name = remove(name, c);
            }
        } else if (areAllWordsUpperCaseBesideDelimiters(name, wordDelimiters)) {
            if (!keepClassesNamesUnaltered) {
                name = WordUtils.capitalizeFully(name, wordDelimiters);
            }
        }

        return name;
    }


    private boolean areAllWordsUpperCaseBesideDelimiters(String words, char... delimiters) {
        char[] wordChars = words.toCharArray();
        for (char c : wordChars) {
            if (!containsAny("" + c, delimiters) && Character.isLowerCase(c)) {
                return false;
            }
        }
        return true;
    }

}
