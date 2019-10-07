package com.araguacaima.braas.api.controller;


import com.araguacaima.braas.api.email.MailSenderFactory;
import com.araguacaima.braas.api.email.MailType;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.Map;

import static com.araguacaima.braas.core.Commons.DEFAULT_ENCODING;

/**
 * Function for use in DRL files.
 */

public class RuleEmailSender {
    private static final Logger log = LoggerFactory.getLogger(RuleEmailSender.class);

    /**
     * Log a trace message from a rule
     */
    public static void send(final String message, final Map<String, Object> parameters) {
        String message1 = message;
        try {
            message1 = URLDecoder.decode(message, DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException ignored) {
        }
        final String formattedMessage = String.format(message1, parameters);

        try {
            Map configs = MongoAccess.getAllAsMap("configs");
            String to = StringUtils.defaultIfBlank((String) parameters.get("to"), configs.get("default-email-to").toString());
            String from = StringUtils.defaultIfBlank((String) parameters.get("from"), configs.get("default-email-from").toString());
            String subject = StringUtils.defaultIfBlank((String) parameters.get("subject"), configs.get("default-email.subject").toString());
            MailSenderFactory.getInstance().getMailSender(MailType.HTML).sendMessage(to, from, subject, Collections.singletonList(message));
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.trace(formattedMessage);
    }

}
