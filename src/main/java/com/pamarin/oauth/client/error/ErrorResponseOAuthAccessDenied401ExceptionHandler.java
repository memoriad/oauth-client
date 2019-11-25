/*
 * Copyright 2017-2019 Pamarin
 */
package com.pamarin.oauth.client.error;

import com.pamarin.core.error.handler.ErrorResponseExceptionHandlerAdapter;
import com.pamarin.core.error.model.ErrorResponse;
import com.pamarin.oauth.client.exception.OAuthAccessDenied401Exception;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 *
 * @author jitta
 */
@Component
public class ErrorResponseOAuthAccessDenied401ExceptionHandler extends ErrorResponseExceptionHandlerAdapter<OAuthAccessDenied401Exception> {

    @Override
    public Class<OAuthAccessDenied401Exception> getTypeClass() {
        return OAuthAccessDenied401Exception.class;
    }

    @Override
    protected Mono<ErrorResponse> buildError(final ServerWebExchange exchange, final OAuthAccessDenied401Exception e) {
        return Mono.fromCallable(() -> {
            return ErrorResponse.unauthorized(e.getMessage());
        });
    }

}
