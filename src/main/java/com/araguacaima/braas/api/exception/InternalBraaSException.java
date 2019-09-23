package com.araguacaima.braas.api.exception;

public class InternalBraaSException extends Exception {
    public InternalBraaSException(Throwable e) {
        super(e);
    }

    public InternalBraaSException(String message) {
        super(message);
    }
}
