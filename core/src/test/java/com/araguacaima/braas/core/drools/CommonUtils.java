package com.araguacaima.braas.core.drools;

import com.araguacaima.commons.utils.StringUtils;
import org.atteo.evo.inflector.English;
import org.springframework.stereotype.Component;

import static com.google.common.base.CaseFormat.*;

/**
 * Created by MB65788 on 25/07/2016.
 */
@Component
public class CommonUtils {

    public String singularizeTypeName(Class clazz) {
        String typeName = cleanClassName(clazz);
        return singularizeTypeName(typeName);
    }

    public String pluralizeTypeName(Class clazz) {
        String typeName = cleanClassName(clazz);
        return pluralizeTypeName(typeName);
    }

    public String singularizeTypeName(String clazz) {
        String typeName = cleanClassName(clazz);

        if (typeName.endsWith("ses")) {
            typeName = typeName.substring(0, typeName.length() - 3);
        } else if (typeName.endsWith("s")) {
            typeName = typeName.substring(0, typeName.length() - 1);
        } else if (typeName.endsWith("ees")) {
            typeName = typeName.substring(0, typeName.length() - 3);
        } else {
            typeName = English.plural(typeName, 1);
        }
        return typeName;
    }

    public String pluralizeTypeName(String clazz) {
        String typeName = cleanClassName(clazz);

        if (!typeName.endsWith("ses") && !typeName.endsWith("ees") && !typeName.endsWith("s")) {
            if (!typeName.endsWith("data") && !typeName.endsWith("Data")) {
                typeName = English.plural(typeName, 2);
            }
        }

        return typeName;
    }

    private String cleanClassName(String simpleName) {
        if (simpleName.startsWith("Dto") || simpleName.startsWith("dto")) {
            simpleName = simpleName.substring(3);
        }
        if (simpleName.endsWith("Dto")) {
            simpleName = simpleName.substring(0, simpleName.length() - 3);
        }
        return StringUtils.uncapitalize(simpleName);
    }

    private String cleanClassName(Class clazz) {
        String simpleName = clazz.getSimpleName();
        return cleanClassName(simpleName);
    }

    public String toSnake(String input) {
        return toSnake(input, true);
    }

    public String toSnake(String input, boolean forceUncapitalized) {
        String snakedInput = LOWER_CAMEL.to(LOWER_HYPHEN, input);
        if (forceUncapitalized) {
            snakedInput = StringUtils.uncapitalize(snakedInput);
        }
        return snakedInput;
    }

    public String splitWords(String input) {
        return splitWords(input, true);
    }

    public String splitWords(String input, boolean forceUncapitalized) {
        String[] underScoredInputList = input.split("(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])");
        String underScoredInput = StringUtils.EMPTY;
        for (String str : underScoredInputList) {
            underScoredInput = underScoredInput + " " + (forceUncapitalized ? StringUtils.uncapitalize(str) : str);
        }
        return underScoredInput;
    }

    public String toUnderscore(String input) {
        return toUnderscore(input, true, true);
    }

    public String toUnderscore(String input, boolean forceUncapitalized, boolean uppercase) {
        String underScoredInput = LOWER_CAMEL.to((uppercase ? UPPER_UNDERSCORE : LOWER_UNDERSCORE), input);
        if (forceUncapitalized) {
            underScoredInput = StringUtils.uncapitalize(underScoredInput);
        }
        return underScoredInput;
    }

    public String fromUnderscore(String input) {
        return fromUnderscore(input, false, false);
    }

    public String fromUnderscore(String input, boolean forceUncapitalized, boolean uppercase) {
        String underScoredInput = UPPER_UNDERSCORE.to((uppercase ? UPPER_CAMEL : LOWER_CAMEL), input);
        if (forceUncapitalized) {
            underScoredInput = StringUtils.uncapitalize(underScoredInput);
        } else {
            underScoredInput = StringUtils.capitalize(underScoredInput);
        }
        return underScoredInput;
    }

}
