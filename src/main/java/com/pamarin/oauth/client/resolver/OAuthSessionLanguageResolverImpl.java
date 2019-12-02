/*
 * Copyright 2017-Current Pamarin.com
 */
package com.pamarin.oauth.client.resolver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import com.pamarin.core.commons.exception.AuthenticationException;
import com.pamarin.oauth.client.constant.OAuthClientConstant;
import com.pamarin.oauth.client.security.OAuthSessionContext;

/**
 *
 * @author jitta
 */
@Component
public class OAuthSessionLanguageResolverImpl implements OAuthSessionLanguageResolver {

    private final OAuthSessionContext sessionContext;

    @Autowired
    public OAuthSessionLanguageResolverImpl(OAuthSessionContext sessionContext) {
        this.sessionContext = sessionContext;
    }

    @Override
    public Mono<String> resolveCode() {
        return sessionContext.getSession()
                .onErrorResume(AuthenticationException.class, e -> Mono.empty())
                .map(session -> session.getLanguage().getCode())
                .defaultIfEmpty(OAuthClientConstant.DEFAULT_LANGUAGE_CODE);
    }

}
