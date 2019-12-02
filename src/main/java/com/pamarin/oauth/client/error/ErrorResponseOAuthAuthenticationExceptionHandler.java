/*
 * Copyright 2017-Current Pamarin.com
 */
package com.pamarin.oauth.client.error;

import com.pamarin.core.error.handler.ErrorResponseExceptionHandlerAdapter;
import com.pamarin.core.error.model.ErrorResponse;
import com.pamarin.oauth.client.exception.OAuthAuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 *
 * @author jitta
 */
@Component
public class ErrorResponseOAuthAuthenticationExceptionHandler extends ErrorResponseExceptionHandlerAdapter<OAuthAuthenticationException> {

    @Override
    public Class<OAuthAuthenticationException> getTypeClass() {
        return OAuthAuthenticationException.class;
    }

    @Override
    protected Mono<ErrorResponse> buildError(final ServerWebExchange exchange, final OAuthAuthenticationException e) {
        return Mono.fromCallable(() -> {
            return ErrorResponse.unauthorized(e.getMessage());
        });
    }

}
