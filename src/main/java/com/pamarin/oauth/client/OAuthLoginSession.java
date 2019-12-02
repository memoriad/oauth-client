/*
 * Copyright 2017-Current Pamarin.com
 */
package com.pamarin.oauth.client;

import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 *
 * @author jitta
 */
public interface OAuthLoginSession {

    Mono<Void> login(String token, ServerWebExchange exchange);

    Mono<Void> loginByAccessToken(String accessToken, ServerWebExchange exchange);

    Mono<Void> loginBySessionToken(String sessionToken, ServerWebExchange exchange);

    Mono<Void> logout(ServerWebExchange exchange);

}
