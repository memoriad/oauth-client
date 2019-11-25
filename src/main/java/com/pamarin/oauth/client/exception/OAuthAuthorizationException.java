/*
 * Copyright 2017-2019 Pamarin
 */
package com.pamarin.oauth.client.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 *
 * @author jitta
 */
@ResponseStatus(code = HttpStatus.FORBIDDEN, reason = "access denied")
public class OAuthAuthorizationException extends RuntimeException {

    public OAuthAuthorizationException(String message) {
        super(message);
    }

    public OAuthAuthorizationException(String message, Throwable cause) {
        super(message, cause);
    }

}
