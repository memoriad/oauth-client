/*
 * Copyright 2017-2019 Pamarin
 */
package com.pamarin.oauth.client.security;

import com.pamarin.oauth.client.model.OAuthSession;
import reactor.core.publisher.Mono;

/**
 *
 * @author jitta
 */
public interface OAuthSessionTokenDecoder {

    Mono<OAuthSession> decode(String sessionToken);

}
