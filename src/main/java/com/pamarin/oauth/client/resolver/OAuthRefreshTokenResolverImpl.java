/*
 * Copyright 2017-2019 Pamarin
 */
package com.pamarin.oauth.client.resolver;

import com.pamarin.core.commons.resolver.HttpCookieResolver;
import com.pamarin.core.commons.resolver.impl.DefaultHttpCookieResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 *
 * @author jitta
 */
@Component
public class OAuthRefreshTokenResolverImpl implements OAuthRefreshTokenResolver {

    private final HttpCookieResolver cookieResolver;

    public OAuthRefreshTokenResolverImpl() {
        this.cookieResolver = new DefaultHttpCookieResolver("refresh_token");
    }

    @Override
    public Mono<String> resolve(final ServerWebExchange exchange) {
        return Mono.fromCallable(() -> cookieResolver.resolve(exchange));
    }

}
