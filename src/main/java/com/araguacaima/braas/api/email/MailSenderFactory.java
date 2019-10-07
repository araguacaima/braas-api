package com.araguacaima.braas.api.email;

import com.araguacaima.braas.api.controller.MongoAccess;
import com.araguacaima.braas.core.google.model.Config;
import com.araguacaima.braas.core.google.wrapper.ConfigWrapper;

import java.io.IOException;
import java.util.Collection;

public class MailSenderFactory {
    private static final MailSenderFactory INSTANCE = new MailSenderFactory();

    public static MailSenderFactory getInstance() {
        return INSTANCE;
    }

    public MailSender getMailSender(MailType type) throws IOException {
        Collection<Config> configs = MongoAccess.getAll(Config.class, "configs");
        switch (type) {
            case SMTP:
                return new SendEmailSMTP(ConfigWrapper.toProperties(configs));
            case ATTACHMENT:
                return new SendEmailAttachment(ConfigWrapper.toProperties(configs));
            default:
                return new SendEmailHTML(ConfigWrapper.toProperties(configs));
        }
    }
}
