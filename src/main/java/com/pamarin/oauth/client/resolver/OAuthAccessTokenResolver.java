/*
 * Copyright 2017-Current Pamarin.com
 */
package com.pamarin.oauth.client.resolver;

import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 *
 * @author jitta
 */
public interface OAuthAccessTokenResolver {
    
    Mono<String> resolve(ServerWebExchange exchange);
    
}
