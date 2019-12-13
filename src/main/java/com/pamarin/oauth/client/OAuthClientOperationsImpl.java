/*
 * Copyright 2017-Current Pamarin.com
 */
package com.pamarin.oauth.client;

import com.pamarin.core.commons.autoconfigure.CoreCommonsProperties;
import com.pamarin.core.commons.exception.AuthenticationException;
import com.pamarin.core.commons.exception.AuthorizationException;
import com.pamarin.core.commons.util.MultiValueMapBuilder;
import com.pamarin.core.error.handler.WebClientExceptionTranslator;
import com.pamarin.oauth.client.autoconfigure.OAuthClientProperties;
import com.pamarin.oauth.client.exception.OAuthAuthenticationException;
import com.pamarin.oauth.client.exception.OAuthAuthorizationException;
import com.pamarin.oauth.client.security.OAuthSessionTokenDecoder;
import com.pamarin.oauth.client.model.OAuthAccessToken;
import com.pamarin.oauth.client.model.OAuthSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 *
 * @author jitta
 */
@Slf4j
public class OAuthClientOperationsImpl implements OAuthClientOperations {

    private final OAuthClientProperties clientProperties;

    private final CoreCommonsProperties commonsProperties;

    private final OAuthSessionTokenDecoder sessionTokenDecoder;

    private final WebClientExceptionTranslator exceptionTranslator;

    public OAuthClientOperationsImpl(
            OAuthClientProperties clientProperties,
            CoreCommonsProperties commonsProperties,
            OAuthSessionTokenDecoder sessionTokenDecoder,
            WebClientExceptionTranslator exceptionTranslator
    ) {
        this.clientProperties = clientProperties;
        this.commonsProperties = commonsProperties;
        this.sessionTokenDecoder = sessionTokenDecoder;
        this.exceptionTranslator = exceptionTranslator;
    }

    @Override
    public Mono<OAuthAccessToken> getAccessTokenByAuthorizationCode(final String authorizationCode) {
        return transformError(
                WebClient.create(authenServerUrl("/oauth/v1/token"))
                        .post()
                        .headers(headers -> headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED))
                        .body(buildCodeBody(authorizationCode))
                        .retrieve()
                        .onStatus(HttpStatus::isError, exceptionTranslator.translate())
                        .bodyToMono(OAuthAccessToken.class)
        );
    }

    @Override
    public Mono<OAuthAccessToken> getAccessTokenByRefreshToken(final String refreshToken) {
        return transformError(
                WebClient.create(authenServerUrl("/oauth/v1/token"))
                        .post()
                        .headers(headers -> headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED))
                        .body(buildRefreshTokenBody(refreshToken))
                        .retrieve()
                        .onStatus(HttpStatus::isError, exceptionTranslator.translate())
                        .bodyToMono(OAuthAccessToken.class)
        );
    }

    @Override
    public Mono<OAuthSession> getSession(final String accessToken) {
        return transformError(
                WebClient.create(authenServerUrl("/oauth/v1/session"))
                        .post()
                        .headers(headers -> {
                            headers.setBearerAuth(accessToken);
                            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                        })
                        .retrieve()
                        .onStatus(HttpStatus::isError, exceptionTranslator.translate())
                        .bodyToMono(OAuthSession.class)
                        .flatMap(session -> sessionTokenDecoder.decode(session.getSessionToken()))
        );
    }

    private String authenServerUrl(final String path) {
        final String url = clientProperties.getAuthorizationServer().getInternalUrl() + path;
        log.debug("oauth url => {}", url);
        return url;
    }

    private BodyInserter buildCodeBody(final String authorizationCode) {
        return BodyInserters.fromFormData(
                MultiValueMapBuilder.newLinkedMultiValueMap()
                        .add("client_id", clientProperties.getId())
                        .add("client_secret", clientProperties.getSecret())
                        .add("grant_type", "authorization_code")
                        .add("redirect_uri", commonsProperties.getApplication().getUrl() + "/oauth/callback")
                        .add("code", authorizationCode)
                        .build()
        );
    }

    private BodyInserter buildRefreshTokenBody(final String refreshToken) {
        return BodyInserters.fromFormData(
                MultiValueMapBuilder.newLinkedMultiValueMap()
                        .add("client_id", clientProperties.getId())
                        .add("client_secret", clientProperties.getSecret())
                        .add("grant_type", "refresh_token")
                        .add("refresh_token", refreshToken)
                        .build()
        );
    }

    private <T> Mono<T> transformError(Mono<T> mono) {
        return mono
                .onErrorResume(AuthenticationException.class, e -> Mono.error(new OAuthAuthenticationException(e.getMessage())))
                .onErrorResume(AuthorizationException.class, e -> Mono.error(new OAuthAuthorizationException(e.getMessage())));
    }
}
