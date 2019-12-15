/*
 * Copyright 2017-Current Pamarin.com
 */
package com.pamarin.oauth.client.security;

import com.pamarin.core.commons.autoconfigure.CoreCommonsProperties;
import com.pamarin.core.commons.resolver.HttpCookieResolver;
import com.pamarin.core.commons.resolver.impl.DefaultHttpCookieResolver;
import com.pamarin.core.commons.util.Base64Utils;
import com.pamarin.oauth.client.exception.OAuthInvalidAuthorizationStateException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 *
 * @author jitta
 */
@Component
public class DefaultOAuthAuthorizationState implements OAuthAuthorizationState {

    private static final int EXPIRES_TIME = 60 * 30; //30 minutes

    private static final String COOKIE_NAME = "authorize_state";

    private static final int STATE_SIZE = 11;

    private final SecureRandom secureRandom;

    private final HttpCookieResolver cookieResolver;

    private final CoreCommonsProperties commonsProperties;

    @Autowired
    public DefaultOAuthAuthorizationState(final CoreCommonsProperties commonsProperties) {
        this.secureRandom = new SecureRandom();
        this.commonsProperties = commonsProperties;
        this.cookieResolver = new DefaultHttpCookieResolver(COOKIE_NAME);
    }

    private String randomState() {
        byte[] bytes = new byte[STATE_SIZE];
        secureRandom.nextBytes(bytes);
        return Base64Utils.encode(bytes);
    }

    @Override
    public Mono<String> create(final ServerWebExchange exchange) {
        return Mono.fromCallable(() -> {
            final String state = randomState();
            final ServerHttpResponse httpResp = exchange.getResponse();
            httpResp.beforeCommit(() -> Mono.fromRunnable(() -> {
                httpResp.addCookie(buildCookie(
                        state,
                        EXPIRES_TIME,
                        exchange
                ));
            }));
            return state;
        });
    }

    @Override
    public Mono<Void> verify(final ServerWebExchange exchange) {
        return Mono.justOrEmpty(exchange.getRequest().getQueryParams().getFirst("state"))
                .flatMap(state -> {
                    final String cookieState = cookieResolver.resolve(exchange);
                    if (!Objects.equals(state, cookieState)) {
                        return Mono.error(new OAuthInvalidAuthorizationStateException(state));
                    }
                    return clear(exchange);
                });
    }

    @Override
    public Mono<Void> clear(final ServerWebExchange exchange) {
        return Mono.fromRunnable(() -> {
            final ServerHttpResponse httpResp = exchange.getResponse();
            httpResp.beforeCommit(() -> Mono.fromRunnable(() -> {
                httpResp.addCookie(buildCookie("", 0, exchange));
            }));
        });
    }

    private ResponseCookie buildCookie(final String state, final int maxAge, final ServerWebExchange exchange) {
        final String applicationUrl = commonsProperties.getApplication().getUrl();
        return ResponseCookie.from(COOKIE_NAME, state)
                .path("/")
                .httpOnly(true)
                .maxAge(maxAge)
                .domain(getDomainName(applicationUrl))
                .secure(applicationUrl.startsWith("https"))
                .build();
    }

    public static String getDomainName(String url) {
        try {
            URI uri = new URI(url);
            String domain = uri.getHost();
            String[] arr = domain.split("\\.");
            if (arr.length == 2) {
                return "www." + domain;
            }
            return domain;
        } catch (URISyntaxException ex) {
            return null;
        }
    }
}
