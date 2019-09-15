package com.araguacaima.braas.core;

import java.util.Map;

/**
 * Created by Alejandro on 21/11/2014.
 */
@SuppressWarnings("WeakerAccess")
public class RuleMessageError extends RuleMessage {
    public MessageType type = MessageType.ERROR;

    public RuleMessageError(String language, String ruleName, String comment, String expectedValue, String parent, String fieldName, Object object) {
        this(language, ruleName, comment, expectedValue, parent, fieldName, object, null);
    }

    public RuleMessageError(String language, String ruleName, String comment, String expectedValue, String parent, String fieldName, Object object, Map<String, Object> context) {
        super(language, ruleName, comment, expectedValue, parent, fieldName, object, context);
    }

    @Override
    public MessageType getType() {
        return type;
    }
}
