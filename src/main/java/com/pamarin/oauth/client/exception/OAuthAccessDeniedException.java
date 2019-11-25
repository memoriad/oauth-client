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
public class OAuthAccessDeniedException extends RuntimeException {

    public OAuthAccessDeniedException(String message) {
        super(message);
    }

    public OAuthAccessDeniedException(String message, Throwable cause) {
        super(message, cause);
    }

}
