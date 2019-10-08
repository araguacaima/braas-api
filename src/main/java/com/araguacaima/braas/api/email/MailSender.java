package com.araguacaima.braas.api.email;

import com.araguacaima.braas.api.model.Email;

import java.util.Collection;

public interface MailSender {

    String sendMessage(String recipientEmail,
                       String from,
                       String subject,
                       Collection<Object> messages);

    String sendMessage(Email email);
}
