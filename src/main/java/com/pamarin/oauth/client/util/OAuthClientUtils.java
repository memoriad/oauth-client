/*
 * Copyright 2017-2019 Pamarin
 */
package com.pamarin.oauth.client.util;

import org.springframework.http.ResponseCookie;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 *
 * @author jitta
 */
public class OAuthClientUtils {

    private OAuthClientUtils() {

    }

    public static void clearTokenCookies(final ServerWebExchange exchange) {
        exchange.getResponse().beforeCommit(() -> Mono.fromRunnable(() -> {
            clearTokenCookie("access_token", exchange);
            clearTokenCookie("refresh_token", exchange);
        }));
    }

    private static void clearTokenCookie(final String cookieName, final ServerWebExchange exchange) {
        exchange.getResponse().addCookie(
                ResponseCookie.from(cookieName, "")
                        .path("/")
                        .httpOnly(true)
                        .secure("https".equalsIgnoreCase(exchange.getRequest().getURI().getScheme()))
                        .sameSite("Lax")
                        .maxAge(0)
                        .build()
        );
    }

}
