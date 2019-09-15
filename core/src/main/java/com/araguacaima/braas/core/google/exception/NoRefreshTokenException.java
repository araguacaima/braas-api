package com.araguacaima.braas.core.google.exception;

/**
 * Exception thrown when no refresh token has been found.
 */
public class NoRefreshTokenException extends GetCredentialsException {

    /**
     * Construct a NoRefreshTokenException.
     *
     * @param authorizationUrl The authorization URL to redirect the user to.
     */
    public NoRefreshTokenException(String authorizationUrl) {
        super(authorizationUrl);
    }

}
