/*
 * Copyright 2017-Current Pamarin.com
 */
package com.pamarin.oauth.client;

import com.pamarin.core.commons.cache.MemoryCacheRepository;
import com.pamarin.core.commons.constant.TokenConstant;
import com.pamarin.core.commons.exception.InvalidTokenException;
import com.pamarin.core.commons.util.Base64Utils;
import com.pamarin.core.commons.util.TokenUtils;
import com.pamarin.oauth.client.model.OAuthAccessToken;
import com.pamarin.oauth.client.model.OAuthSession;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.thymeleaf.util.StringUtils;
import reactor.core.publisher.Mono;

/**
 *
 * @author jitta
 */
@Slf4j
public class SessionCacheOAuthClientOperations implements OAuthClientOperations {

    private final Duration cacheTimeout;

    private final OAuthClientOperations clientOperations;

    private final MemoryCacheRepository memoryCacheRepository;

    public SessionCacheOAuthClientOperations(
            OAuthClientOperations clientOperations,
            MemoryCacheRepository memoryCacheRepository,
            Duration cacheTimeout
    ) {
        this.cacheTimeout = cacheTimeout;
        this.clientOperations = clientOperations;
        this.memoryCacheRepository = memoryCacheRepository;
    }

    @Override
    public Mono<OAuthAccessToken> getAccessTokenByAuthorizationCode(final String authorizationCode) {
        return clientOperations.getAccessTokenByAuthorizationCode(authorizationCode);
    }

    @Override
    public Mono<OAuthAccessToken> getAccessTokenByRefreshToken(final String refreshToken) {
        return clientOperations.getAccessTokenByRefreshToken(refreshToken);
    }

    private String makeCacheKey(final String id) {
        return "oauth_session:" + id;
    }

    @Override
    public Mono<OAuthSession> getSession(final String accessToken) {
        return getTokenId(accessToken)
                .flatMap(tokenId -> {
                    final String cacheKey = makeCacheKey(tokenId);
                    return memoryCacheRepository.<OAuthSession>get(cacheKey)
                            .switchIfEmpty(
                                    clientOperations.getSession(accessToken)
                                            .flatMap(session -> {
                                                return memoryCacheRepository.set(cacheKey, session, cacheTimeout)
                                                        .thenReturn(session);
                                            })
                            );
                });
    }

    private Mono<String> getTokenId(final String rawAccessToken) {
        return Mono.defer(() -> {
            try {
                final String accessToken = TokenUtils.removePrefix(rawAccessToken, TokenConstant.ACCESS_TOKEN_PREFIX);
                final String rawToken = Base64Utils.decode(accessToken);
                final String[] arr = StringUtils.split(rawToken, ":");
                return Mono.just(arr[0]);
            } catch (Exception e) {
                return Mono.error(new InvalidTokenException("invalid access_token"));
            }
        });
    }
}
