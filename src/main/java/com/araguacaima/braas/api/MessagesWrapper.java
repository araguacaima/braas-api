package com.araguacaima.braas.api;

import com.araguacaima.braas.api.common.Commons;
import com.araguacaima.braas.api.common.Message;
import com.araguacaima.braas.api.common.MessageSummary;
import com.araguacaima.braas.api.common.Messages;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

public class MessagesWrapper {

    public static Messages fromSpecificationMapToMessages(Map<Object, Object> specificationMap) {
        Messages messages = new Messages();
        if (specificationMap == null) {
            return messages;
        }
        String code = Commons.SPECIFICATION_ERROR;
        Object value = specificationMap.get(code);
        fillMessage(messages, code, value);
        code = Commons.SPECIFICATION_ERROR_CREATION;
        value = specificationMap.get(code);
        fillMessage(messages, code, value);
        specificationMap.get(code);
        code = Commons.SPECIFICATION_ERROR_MODIFICATION;
        value = specificationMap.get(code);
        fillMessage(messages, code, value);
        specificationMap.get(code);
        code = Commons.SPECIFICATION_ERROR_DELETION;
        value = specificationMap.get(code);
        fillMessage(messages, code, value);
        specificationMap.get(code);
        code = Commons.SPECIFICATION_ERROR_REPLACEMENT;
        value = specificationMap.get(code);
        fillMessage(messages, code, value);
        specificationMap.get(code);
        code = Commons.SPECIFICATION_ERROR_ALREADY_EXISTS;
        value = specificationMap.get(code);
        fillMessage(messages, code, value);
        MessageSummary summary = new MessageSummary();
        summary.setTotalMessages(messages.getMessages().size());
        messages.setSummary(summary);
        return messages;
    }

    public static void fillMessage(Messages messages, String code, Object value) {
        Message message = new Message();
        message.setCode(code);
        message.setMessage(value != null ? value.toString() : null);
        messages.addMessage(message);
    }

    public static Messages fromExceptionToMessages(Throwable ex, int status) {
        Messages messages = new Messages();
        String code = String.valueOf(status);
        fillMessage(messages, code, ex != null && !StringUtils.isEmpty(ex.getMessage()) ? ex.getMessage() : null);
        return messages;
    }
}
