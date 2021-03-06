/*
 * Copyright 2017-Current Pamarin.com
 */
package com.pamarin.oauth.client.security;

import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 *
 * @author jitta
 */
public interface OAuthAuthorizationState {

    Mono<String> create(ServerWebExchange exchange);

    Mono<Void> verify(ServerWebExchange exchange);

    Mono<Void> clear(ServerWebExchange exchange);

}
