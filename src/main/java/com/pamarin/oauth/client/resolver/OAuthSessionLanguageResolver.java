/*
 * Copyright 2017-2019 Pamarin
 */
package com.pamarin.oauth.client.resolver;

import reactor.core.publisher.Mono;

/**
 *
 * @author jitta
 */
public interface OAuthSessionLanguageResolver {

    Mono<String> resolveCode();

}
