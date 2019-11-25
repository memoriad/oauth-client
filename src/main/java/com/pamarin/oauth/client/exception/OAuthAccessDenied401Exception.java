/*
 * Copyright 2017-2019 Pamarin
 */
package com.pamarin.oauth.client.exception;

/**
 *
 * @author jitta
 */
public class OAuthAccessDenied401Exception extends RuntimeException {

    public OAuthAccessDenied401Exception(String message) {
        super(message);
    }

    public OAuthAccessDenied401Exception(String message, Throwable cause) {
        super(message, cause);
    }

}
