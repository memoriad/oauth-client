/*
 * Copyright 2017-Current Pamarin.com
 */
package com.pamarin.oauth.client.error;

import com.pamarin.core.error.handler.ErrorResponseExceptionHandlerAdapter;
import com.pamarin.core.error.model.ErrorResponse;
import com.pamarin.oauth.client.exception.OAuthAuthorizationException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 *
 * @author jitta
 */
@Component
public class ErrorResponseOAuthAuthorizationExceptionHandler extends ErrorResponseExceptionHandlerAdapter<OAuthAuthorizationException> {

    @Override
    public Class<OAuthAuthorizationException> getTypeClass() {
        return OAuthAuthorizationException.class;
    }

    @Override
    protected Mono<ErrorResponse> buildError(final ServerWebExchange exchange, final OAuthAuthorizationException e) {
        return Mono.fromCallable(() -> {
            return ErrorResponse.accessDenied(e.getMessage());
        });
    }

}
