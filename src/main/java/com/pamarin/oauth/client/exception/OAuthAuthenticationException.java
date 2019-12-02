/*
 * Copyright 2017-Current Pamarin.com
 */
package com.pamarin.oauth.client.exception;

/**
 *
 * @author jitta
 */
public class OAuthAuthenticationException extends RuntimeException {

    public OAuthAuthenticationException(String message) {
        super(message);
    }

    public OAuthAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }

}
