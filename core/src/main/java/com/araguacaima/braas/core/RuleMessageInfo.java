package com.araguacaima.braas.core;

import java.util.Map;

/**
 * Created by Alejandro on 08/12/2014.
 */
@SuppressWarnings("WeakerAccess")
public class RuleMessageInfo extends RuleMessage {

    public MessageType type = MessageType.INFO;

    public RuleMessageInfo(String language, String ruleName, String comment, String expectedValue, String parent, String fieldName, Object object) {
        this(language, ruleName, comment, expectedValue, parent, fieldName, object, null);
    }

    public RuleMessageInfo(String language, String ruleName, String comment, String expectedValue, String parent, String fieldName, Object object, Map<String, Object> context) {
        super(language, ruleName, comment, expectedValue, parent, fieldName, object, context);
    }

    @Override
    public MessageType getType() {
        return type;
    }
}
