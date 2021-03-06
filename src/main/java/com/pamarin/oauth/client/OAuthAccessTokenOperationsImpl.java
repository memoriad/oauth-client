/*
 * Copyright 2017-Current Pamarin.com
 */
package com.pamarin.oauth.client;

import com.pamarin.core.commons.autoconfigure.CoreCommonsProperties;
import com.pamarin.oauth.client.exception.OAuthAuthenticationException;
import com.pamarin.oauth.client.exception.OAuthAuthorizationException;
import com.pamarin.oauth.client.model.OAuthAccessToken;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import static org.springframework.util.StringUtils.hasText;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 *
 * @author jitta
 */
@Component
public class OAuthAccessTokenOperationsImpl implements OAuthAccessTokenOperations {

    private static final int ONE_DAY_SECONDS = 60 * 60 * 24;

    private static final int FOURTEEN_DAYS_SECONDS = ONE_DAY_SECONDS * 14;

    private static final String ERROR_MESSAGE = "please authorize";

    private final OAuthClientOperations clientOperations;
    
    private final CoreCommonsProperties commonsProperties;

    @Autowired
    public OAuthAccessTokenOperationsImpl(
            final OAuthClientOperations clientOperations,
            final CoreCommonsProperties commonsProperties
    ) {
        this.clientOperations = clientOperations;
        this.commonsProperties = commonsProperties;
    }

    @Override
    public Mono<OAuthAccessToken> getAccessTokenByAuthorizationCode(final String code, final ServerWebExchange exchange) {
        if (!hasText(code)) {
            return Mono.error(new OAuthAuthorizationException(ERROR_MESSAGE));
        }
        return clientOperations.getAccessTokenByAuthorizationCode(code)
                .onErrorResume(OAuthAuthenticationException.class, e -> Mono.error(new OAuthAuthorizationException(e.getMessage())))
                .doOnNext(accessToken -> storeToken(accessToken, exchange));
    }

    @Override
    public Mono<OAuthAccessToken> getAccessTokenByRefreshToken(final String refreshToken, final ServerWebExchange exchange) {
        if (!hasText(refreshToken)) {
            return Mono.error(new OAuthAuthorizationException(ERROR_MESSAGE));
        }
        return clientOperations.getAccessTokenByRefreshToken(refreshToken)
                .onErrorResume(OAuthAuthenticationException.class, e -> Mono.error(new OAuthAuthorizationException(e.getMessage())))
                .doOnNext(accessToken -> storeToken(accessToken, exchange));
    }

    private void storeToken(final OAuthAccessToken accessToken, final ServerWebExchange exchange) {
        final ServerHttpResponse httpResp = exchange.getResponse();
        
        final Map<String, Object> attributes = exchange.getAttributes();
        attributes.put("access_token", accessToken.getAccessToken());
        attributes.put("refresh_token", accessToken.getRefreshToken());
        
        httpResp.beforeCommit(() -> Mono.fromRunnable(() -> {
            httpResp.addCookie(buildCookie(
                    "access_token",
                    accessToken.getAccessToken(),
                    ONE_DAY_SECONDS,
                    exchange
            ));

            httpResp.addCookie(buildCookie(
                    "refresh_token",
                    accessToken.getRefreshToken(),
                    FOURTEEN_DAYS_SECONDS,
                    exchange
            ));
        }));
    }

    private ResponseCookie buildCookie(final String cookieName, final String cookieValue, final int maxAge, final ServerWebExchange exchange) {
        return ResponseCookie.from(cookieName, cookieValue)
                .path("/")
                .httpOnly(true)
                .secure(commonsProperties.getApplication().getUrl().startsWith("https"))
                .sameSite("Lax")
                .maxAge(maxAge)
                .build();
    }
}
