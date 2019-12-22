/*
 * Copyright 2017-Current Pamarin.com
 */
package com.pamarin.oauth.client.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.pamarin.core.commons.constant.TokenConstant;
import com.pamarin.core.commons.exception.InvalidTokenException;
import com.pamarin.core.commons.security.RSAKeyPairs;
import com.pamarin.core.commons.util.TokenUtils;
import com.pamarin.oauth.client.model.OAuthSession;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 *
 * @author jitta
 */
@Component
public class OAuthSessionTokenVerifierImpl implements OAuthSessionTokenVerifier {

    private final RSAKeyPairs rsaKeyPairs;

    @Autowired
    public OAuthSessionTokenVerifierImpl(RSAKeyPairs rsaKeyPairs) {
        this.rsaKeyPairs = rsaKeyPairs;
    }

    @Override
    public Mono<OAuthSession> verify(final String sessionToken) {
        return rsaKeyPairs.getPublicKey()
                .flatMap(publicKey -> verifyToken(
                        publicKey, 
                        TokenUtils.removePrefix(sessionToken, TokenConstant.SESSION_TOKEN_PREFIX)
                ))
                .map(payload -> convertToSession(payload, sessionToken));

    }

    private Mono<DecodedJWT> verifyToken(final RSAPublicKey publicKey, final String sessionToken) {
        try {
            return Mono.just(
                    JWT.require(Algorithm.RSA256(publicKey, null))
                            .build()
                            .verify(sessionToken)
            );
        } catch (JWTVerificationException e) {
            return Mono.error(new InvalidTokenException("invalid session_token", e));
        }
    }
    
    private OAuthSession convertToSession(final DecodedJWT payload, final String sessionToken) {
        final Long issuedAt = payload.getClaim("issuedAt").asLong();
        final Long expiresAt = payload.getClaim("expiresAt").asLong();
        final Long lastAccessedAt = payload.getClaim("lastAccessedAt").asLong();
        final Long loggedinAt = payload.getClaim("loggedinAt").asLong();
        final Long confirmPasswordAt = payload.getClaim("confirmPasswordAt").asLong();
        
        return OAuthSession.builder()
                .sessionToken(sessionToken)
                .id(payload.getClaim("id").asString())
                .issuedAt(issuedAt == null ? 0 : issuedAt)
                .expiresAt(expiresAt == null ? 0 : expiresAt)
                .lastAccessedAt(lastAccessedAt == null ? 0 : lastAccessedAt)
                .loggedinAt(loggedinAt == null ? 0 : loggedinAt)
                .confirmPasswordAt(confirmPasswordAt == null ? 0 : confirmPasswordAt)
                .user(
                        OAuthSession.User.builder()
                                .id(payload.getClaim("user_id").asString())
                                .name(payload.getClaim("user_name").asString())
                                .photoUrl(payload.getClaim("user_photo_url").asString())
                                .username(payload.getClaim("user_username").asString())
                                .authorities(Arrays.asList(payload.getClaim("user_authorities").asArray(String.class)))
                                .build()
                )
                .client(
                        OAuthSession.Client.builder()
                                .id(payload.getClaim("client_id").asString())
                                .name(payload.getClaim("client_name").asString())
                                .scopes(Arrays.asList(payload.getClaim("client_scopes").asArray(String.class)))
                                .build()
                )
                .build();
    }
}
