package com.araguacaima.braas.api.controller;


import com.araguacaima.braas.api.email.MailSender;
import com.araguacaima.braas.api.email.MailSenderFactory;
import com.araguacaima.braas.api.email.MailType;
import com.araguacaima.braas.api.exception.InternalBraaSException;

import java.util.Collection;


public class EmailService {

    private MailSenderFactory mailSenderFactory = MailSenderFactory.getInstance();
    private MailSender mailSender;

    /*
     * Send HTML mail (simple)
     */
    public void sendSimpleMail(String recipientsList,
                               String from, String subject,
                               Collection<Object> messages) throws InternalBraaSException {
        try {
            mailSenderFactory.getMailSender(MailType.HTML).sendMessage(recipientsList, from, subject, messages);
        } catch (Throwable t) {
            t.printStackTrace();
            throw new InternalBraaSException(t);
        }
    }
}
