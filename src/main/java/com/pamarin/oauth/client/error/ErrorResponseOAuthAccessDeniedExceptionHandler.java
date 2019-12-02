/*
 * Copyright 2017-Current Pamarin.com
 */
package com.pamarin.oauth.client.error;

import com.pamarin.core.error.handler.ErrorResponseExceptionHandlerAdapter;
import com.pamarin.core.error.model.ErrorResponse;
import com.pamarin.oauth.client.exception.OAuthAccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 *
 * @author jitta
 */
@Component
public class ErrorResponseOAuthAccessDeniedExceptionHandler extends ErrorResponseExceptionHandlerAdapter<OAuthAccessDeniedException> {

    @Override
    public Class<OAuthAccessDeniedException> getTypeClass() {
        return OAuthAccessDeniedException.class;
    }

    @Override
    protected Mono<ErrorResponse> buildError(final ServerWebExchange exchange, final OAuthAccessDeniedException e) {
        return Mono.fromCallable(() -> {
            return ErrorResponse.accessDenied(e.getMessage());
        });
    }

}
