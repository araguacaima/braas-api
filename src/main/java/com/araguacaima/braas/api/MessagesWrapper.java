package com.araguacaima.braas.api;

import com.araguacaima.braas.api.common.Message;
import com.araguacaima.braas.api.common.Messages;
import org.apache.commons.lang3.StringUtils;

public class MessagesWrapper {

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
