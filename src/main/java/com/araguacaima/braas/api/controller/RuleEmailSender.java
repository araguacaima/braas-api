package com.araguacaima.braas.api.controller;


import com.araguacaima.braas.core.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import static com.araguacaima.braas.core.Commons.DEFAULT_ENCODING;
import static com.araguacaima.braas.core.Commons.enumsUtils;

/**
 * Function for use in DRL files.
 */

public class RuleEmailSender {
    private static final Logger log = LoggerFactory.getLogger(RuleEmailSender.class);

    /**
     * Log a trace message from a rule
     */
    public static void trace(final String message, final Object... parameters) {
        String message1 = message;
        try {
            message1 = URLDecoder.decode(message, DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException ignored) {
        }
        final String formattedMessage = String.format(message1, parameters);
        log.trace(formattedMessage);
    }

    /**
     * Log a info message from a rule
     */
    public static void info(final String message, final Object... parameters) {
        String message1 = message;
        try {
            message1 = URLDecoder.decode(message, DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException ignored) {
        }
        final String formattedMessage = String.format(message1, parameters);
        log.info(formattedMessage);
    }

    /**
     * Log a debug message from a rule
     */
    public static void debug(final String message, final Object... parameters) {
        String message1 = message;
        try {
            message1 = URLDecoder.decode(message, DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException ignored) {
        }
        final String formattedMessage = String.format(message1, parameters);
        log.debug(formattedMessage);
    }

    /**
     * Log a error message from a rule
     */
    public static void error(final String message, final Object... parameters) {
        String message1 = message;
        try {
            message1 = URLDecoder.decode(message, DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException ignored) {
        }
        final String formattedMessage = String.format(message1, parameters);
        log.error(formattedMessage);
    }

    /**
     * Log a warn message from a rule
     */
    public static void warn(final String message, final Object... parameters) {
        String message1 = message;
        try {
            message1 = URLDecoder.decode(message, DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException ignored) {
        }
        final String formattedMessage = String.format(message1, parameters);
        log.warn(formattedMessage);
    }

    /**
     * Log a message from a rule
     */
    public static void log(String type_, final String message, final Object... parameters) {
        String message1 = message;
        try {
            message1 = URLDecoder.decode(message, DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException ignored) {
        }
        try {
            MessageType type = enumsUtils.getEnum(MessageType.class, type_);
            log(type, message1, parameters);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Log a message from a rule
     */
    public static void log(MessageType type, final String message, final Object... parameters) {

        String message1 = message;
        try {
            message1 = URLDecoder.decode(message, DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException ignored) {
        }
        switch (type) {
            case SUCCESS:
                info(message1, parameters);
                break;
            case DEBUG:
                debug(message1, parameters);
                break;
            case WARNING:
                warn(message1, parameters);
                break;
            case ERROR:
                error(message1, parameters);
                break;
            case INFO:
                info(message1, parameters);
                break;
            default:
                info(message1, parameters);
                break;
        }
    }

}
