/*
 * Copyright 2017-2019 Pamarin
 */
package com.pamarin.oauth.client.controller;

import com.pamarin.core.commons.autoconfigure.CoreCommonsProperties;
import com.pamarin.core.commons.util.HttpUtils;
import com.pamarin.core.commons.util.QuerystringBuilder;
import com.pamarin.core.error.handler.WebClientExceptionTranslator;
import com.pamarin.oauth.client.autoconfigure.OAuthClientProperties;
import com.pamarin.oauth.client.util.OAuthClientUtils;
import static java.lang.String.format;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 *
 * @author jitta
 */
@Slf4j
@Controller
public class SignoutController {

    private final WebClient webClient;

    private final OAuthClientProperties clientProperties;

    private final CoreCommonsProperties commonsProperties;

    private final WebClientExceptionTranslator exceptionTranslator;

    @Autowired
    public SignoutController(
            OAuthClientProperties clientProperties,
            CoreCommonsProperties commonsProperties,
            WebClientExceptionTranslator exceptionTranslator,
            @Qualifier("oauthWebClient") WebClient webClient
    ) {
        this.webClient = webClient;
        this.clientProperties = clientProperties;
        this.commonsProperties = commonsProperties;
        this.exceptionTranslator = exceptionTranslator;
    }

    private String getSignoutUrl() {
        return format("%s/oauth/signout?%s",
                clientProperties.getAuthorizationServer().getUrl(),
                new QuerystringBuilder()
                        .addParameter("client_id", clientProperties.getId())
                        .addParameter("redirect_uri", commonsProperties.getApplication().getUrl())
                        .build()
        );
    }

    private Mono<Void> signoutFromBackendService(final ServerWebExchange exchange) {
        return webClient.post()
                .uri(clientProperties.getAuthorizationServer().getInternalUrl() + "/oauth/v1/signout")
                .headers(headers -> {
                    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                })
                .retrieve()
                .onStatus(HttpStatus::isError, exceptionTranslator.translate())
                .bodyToMono(String.class)
                .doOnSuccess(none -> {
                    OAuthClientUtils.clearTokenCookies(exchange);
                    HttpUtils.redirectTo(
                            exchange.getResponse(),
                            commonsProperties.getApplication().getUrl()
                    );
                })
                .then();
    }

    @GetMapping("/oauth/signout")
    public Mono<Void> signout(final ServerWebExchange exchange) {
        return signoutFromBackendService(exchange)
                .onErrorResume(Exception.class, e -> {
                    return Mono.fromRunnable(() -> {
                        OAuthClientUtils.clearTokenCookies(exchange);
                        HttpUtils.redirectTo(exchange.getResponse(), getSignoutUrl());
                    });
                });
    }
}
