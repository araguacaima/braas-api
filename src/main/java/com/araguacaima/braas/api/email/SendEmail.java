package com.araguacaima.braas.api.email;

import com.araguacaima.braas.api.model.Email;

import java.util.LinkedHashSet;
import java.util.Set;

public abstract class SendEmail implements MailSender {

    @Override
    public String sendMessage(Email email) {
        Set<Object> messages = new LinkedHashSet<>();
        messages.add("Name: " + email.getName());
        messages.add("Phone: " + email.getPhone());
        messages.add("Email: " + email.getEmail());
        messages.add("Message: " + email.getMessage());
        return sendMessage(email.getTo(), email.getFrom(), "Contact", messages);
    }
}
