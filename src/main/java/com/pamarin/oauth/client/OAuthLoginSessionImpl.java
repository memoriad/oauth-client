/*
 * Copyright 2017-2019 Pamarin
 */
package com.pamarin.oauth.client;

import com.pamarin.core.commons.constant.TokenConstant;
import com.pamarin.core.commons.security.DefaultUserDetails;
import static com.pamarin.oauth.client.constant.OAuthClientConstant.OAUTH_SECURITY_CONTEXT;
import static com.pamarin.oauth.client.constant.OAuthClientConstant.OAUTH_SESSION_CONTEXT;
import com.pamarin.oauth.client.exception.OAuthAuthenticationException;
import com.pamarin.oauth.client.model.OAuthSession;
import com.pamarin.oauth.client.security.OAuthSessionTokenVerifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import static org.springframework.util.StringUtils.hasText;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 *
 * @author jitta
 */
@Slf4j
@Component
public class OAuthLoginSessionImpl implements OAuthLoginSession {

    private final OAuthClientOperations clientOperations;

    private final OAuthSessionTokenVerifier sessionTokenVerifier;

    private final ServerSecurityContextRepository securityContextRepository;

    @Autowired
    public OAuthLoginSessionImpl(
            OAuthClientOperations clientOperations,
            OAuthSessionTokenVerifier sessionTokenVerifier,
            ServerSecurityContextRepository securityContextRepository
    ) {
        this.clientOperations = clientOperations;
        this.sessionTokenVerifier = sessionTokenVerifier;
        this.securityContextRepository = securityContextRepository;
    }

    @Override
    public Mono<Void> login(final String token, final ServerWebExchange exchange) {
        if (isSessionToken(token)) {
            return loginBySessionToken(token, exchange);
        }
        return loginByAccessToken(token, exchange);
    }

    private boolean isSessionToken(String token) {
        return hasText(token) && token.startsWith(TokenConstant.SESSION_TOKEN_PREFIX);
    }

    @Override
    public Mono<Void> loginByAccessToken(final String accessToken, final ServerWebExchange exchange) {
        if (!hasText(accessToken)) {
            return doLogout(exchange);
        }
        return clientOperations.getSession(accessToken)
                .flatMap(session -> savePrincipal(session, exchange));
    }

    @Override
    public Mono<Void> loginBySessionToken(final String sessionToken, final ServerWebExchange exchange) {
        if (!hasText(sessionToken)) {
            return doLogout(exchange);
        }
        return sessionTokenVerifier.verify(sessionToken)
                .flatMap(session -> savePrincipal(session, exchange));
    }

    private Mono<Void> doLogout(final ServerWebExchange exchange) {
        return logout(exchange)
                .then(Mono.error(new OAuthAuthenticationException("Unauthorized")));
    }

    @Override
    public Mono<Void> logout(final ServerWebExchange exchange) {
        return Mono.fromRunnable(() -> {
            exchange.getAttributes().remove(OAUTH_SECURITY_CONTEXT);
            exchange.getAttributes().remove(OAUTH_SESSION_CONTEXT);
        });
    }

    private Mono<Void> savePrincipal(final OAuthSession session, final ServerWebExchange exchange) {
        return securityContextRepository.save(exchange, convertToSecurityContext(session.getUser()))
                .then(Mono.fromRunnable(() -> {
                    exchange.getAttributes().put(OAUTH_SESSION_CONTEXT, session);
                }));
    }

    private SecurityContext convertToSecurityContext(final OAuthSession.User user) {
        DefaultUserDetails userDetails = DefaultUserDetails.builder()
                .username(user.getId())
                .password("*****")
                .authorities(user.getAuthorities())
                .build();

        SecurityContext context = new SecurityContextImpl();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        ));
        return context;
    }
}
