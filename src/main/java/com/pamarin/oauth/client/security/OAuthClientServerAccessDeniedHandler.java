/*
 * Copyright 2017-Current Pamarin.com
 */
package com.pamarin.oauth.client.security;

import com.pamarin.oauth.client.exception.OAuthAccessDeniedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpCookie;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.util.MultiValueMap;
import static org.springframework.util.StringUtils.hasText;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import com.pamarin.oauth.client.exception.OAuthAccessDenied401Exception;

/**
 *
 * @author jitta
 */
@Slf4j
public class OAuthClientServerAccessDeniedHandler implements ServerAccessDeniedHandler {

    @Override
    public Mono<Void> handle(final ServerWebExchange exchange, final AccessDeniedException e) {
        if (isAuthenticated(exchange)) {
            return Mono.error(new OAuthAccessDeniedException("Access Denied", e));
        }
        return Mono.error(new OAuthAccessDenied401Exception("Unauthorized", e));
    }

    private boolean isAuthenticated(final ServerWebExchange exchange) {
        return hasAuthorization(exchange)
                || hasCookie(exchange);
    }

    private boolean hasAuthorization(final ServerWebExchange exchange) {
        return hasText(exchange.getRequest().getHeaders().getFirst("Authorization"));
    }

    private boolean hasCookie(final ServerWebExchange exchange) {
        final MultiValueMap<String, HttpCookie> cookies = exchange.getRequest().getCookies();
        return (cookies.getFirst("access_token") != null)
                || (cookies.getFirst("refresh_token") != null);
    }
}
