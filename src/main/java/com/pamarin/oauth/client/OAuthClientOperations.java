/*
 * Copyright 2017-2019 Pamarin
 */
package com.pamarin.oauth.client;

import com.pamarin.oauth.client.model.OAuthAccessToken;
import com.pamarin.oauth.client.model.OAuthSession;
import reactor.core.publisher.Mono;

/**
 *
 * @author jitta
 */
public interface OAuthClientOperations {

    Mono<OAuthAccessToken> getAccessTokenByAuthorizationCode(String authorizationCode);

    Mono<OAuthAccessToken> getAccessTokenByRefreshToken(String refreshToken);
  
    Mono<OAuthSession> getSession(String accessToken);

}
