/*
 * Copyright 2017-Current Pamarin.com
 */
package com.pamarin.oauth.client.controller;

import com.pamarin.oauth.client.model.OAuthSession;
import com.pamarin.oauth.client.security.OAuthSessionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 *
 * @author jitta
 */
@RestController
public class SessionController {

    private final OAuthSessionContext sessionContext;

    @Autowired
    public SessionController(final OAuthSessionContext sessionContext) {
        this.sessionContext = sessionContext;
    }

    @GetMapping("/oauth/session")
    public Mono<OAuthSession> getSession() {
        return sessionContext.getSession();
    }
}
