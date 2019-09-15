package com.araguacaima.braas.core;


import com.araguacaima.commons.utils.EnumsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Function for use in DRL files.
 */

public class RuleLogging {
    private static final Logger log = LoggerFactory.getLogger(RuleLogging.class);
    private static final EnumsUtils<MessageType> ENUMS_UTILS = new EnumsUtils<>();

    /**
     * Log a trace message from a rule
     */
    public static void trace(final String message, final Object... parameters) {

        final String formattedMessage = String.format(message, parameters);
        log.trace(formattedMessage);
    }

    /**
     * Log a info message from a rule
     */
    public static void info(final String message, final Object... parameters) {

        final String formattedMessage = String.format(message, parameters);
        log.info(formattedMessage);
    }

    /**
     * Log a debug message from a rule
     */
    public static void debug(final String message, final Object... parameters) {

        final String formattedMessage = String.format(message, parameters);
        log.debug(formattedMessage);
    }

    /**
     * Log a error message from a rule
     */
    public static void error(final String message, final Object... parameters) {

        final String formattedMessage = String.format(message, parameters);
        log.error(formattedMessage);
    }

    /**
     * Log a warn message from a rule
     */
    public static void warn(final String message, final Object... parameters) {

        final String formattedMessage = String.format(message, parameters);
        log.warn(formattedMessage);
    }

    /**
     * Log a message from a rule
     */
    public static void log(String type_, final String message, final Object... parameters) {
        try {
            MessageType type = ENUMS_UTILS.getEnum(MessageType.class, type_);
            log(type, message, parameters);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Log a message from a rule
     */
    public static void log(MessageType type, final String message, final Object... parameters) {

        switch (type) {
            case SUCCESS:
                info(message, parameters);
                break;
            case DEBUG:
                debug(message, parameters);
                break;
            case WARNING:
                warn(message, parameters);
                break;
            case ERROR:
                error(message, parameters);
                break;
            case INFO:
                info(message, parameters);
                break;
            default:
                info(message, parameters);
                break;
        }
    }

}
