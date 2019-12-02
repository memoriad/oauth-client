/*
 * Copyright 2017-Current Pamarin.com
 */
package com.pamarin.oauth.client;

import com.pamarin.oauth.client.model.OAuthAccessToken;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 *
 * @author jitta
 */
public interface OAuthAccessTokenOperations { 
    
    Mono<OAuthAccessToken> getAccessTokenByAuthorizationCode(String code, ServerWebExchange exchange);

    Mono<OAuthAccessToken> getAccessTokenByRefreshToken(String refreshToken, ServerWebExchange exchange);  
    
}
