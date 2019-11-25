/*
 * Copyright 2017-2019 Pamarin
 */
package com.pamarin.oauth.client.exception;

/**
 *
 * @author jitta
 */
public class OAuthInvalidAuthorizationStateException extends RuntimeException {

    private final String state;

    public OAuthInvalidAuthorizationStateException(String state) {
        super("invalid state \"" + state + "\"");
        this.state = state;
    }

    public String getState() {
        return state;
    }

}
