/*
 * Copyright 2017-2019 Pamarin
 */
package com.pamarin.oauth.client.security;

import com.pamarin.core.commons.security.AnonymousAuthentication;
import static com.pamarin.oauth.client.constant.OAuthClientConstant.OAUTH_SECURITY_CONTEXT;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 *
 * @author jitta
 */
@Slf4j
public class OAuthClientServerSecurityContextRepository implements ServerSecurityContextRepository {

    private static final Authentication ANONYMOUS = new AnonymousAuthentication();

    @Override
    public Mono<Void> save(final ServerWebExchange exchange, final SecurityContext context) {
        return Mono.fromRunnable(() -> {
            final Map<String, Object> attributes = exchange.getAttributes();
            if (context == null) {
                attributes.remove(OAUTH_SECURITY_CONTEXT);
            } else {
                attributes.put(OAUTH_SECURITY_CONTEXT, context);
            }
        });
    }

    @Override
    public Mono<SecurityContext> load(final ServerWebExchange exchange) {
        return Mono.fromCallable(() -> {
            final SecurityContext context = (SecurityContext) exchange.getAttributes().get(OAUTH_SECURITY_CONTEXT);
            if (context == null) {
                return new SecurityContextImpl(ANONYMOUS);
            }
            return context;
        });
    }
}
