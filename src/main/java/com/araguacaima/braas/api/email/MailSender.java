package com.araguacaima.braas.api.email;

import java.util.Collection;

public interface MailSender {

    String sendMessage(String recipientEmail,
                       String from, String subject,
                       Collection<Object> messages);
}
