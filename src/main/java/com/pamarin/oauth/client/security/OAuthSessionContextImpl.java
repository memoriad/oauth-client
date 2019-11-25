/*
 * Copyright 2017-2019 Pamarin
 */
package com.pamarin.oauth.client.security;

import com.pamarin.core.commons.exception.AuthenticationException;
import com.pamarin.core.commons.provider.ServerWebExchangeProvider;
import static com.pamarin.oauth.client.constant.OAuthClientConstant.OAUTH_SESSION_CONTEXT;
import com.pamarin.oauth.client.model.OAuthSession;
import reactor.core.publisher.Mono;

/**
 *
 * @author jitta
 */
public class OAuthSessionContextImpl implements OAuthSessionContext {

    private final ServerWebExchangeProvider exchangeProvider;

    public OAuthSessionContextImpl(ServerWebExchangeProvider exchangeProvider) {
        this.exchangeProvider = exchangeProvider;
    }

    @Override
    public Mono<OAuthSession> getSession() {
        return exchangeProvider.provide()
                .flatMap(exchange -> {
                    final OAuthSession session = exchange.getAttribute(OAUTH_SESSION_CONTEXT);
                    if (session == null) {
                        return Mono.empty();
                    }
                    return Mono.just(session);
                })
                .switchIfEmpty(Mono.error(new AuthenticationException("Unauthorized")));
    }

}
