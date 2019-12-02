/*
 * Copyright 2017-Current Pamarin.com
 */
package com.pamarin.oauth.client.filter;

import static com.pamarin.oauth.client.constant.OAuthClientConstant.OAUTH_SESSION_CONTEXT;
import java.util.NoSuchElementException;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;
import com.pamarin.oauth.client.model.OAuthSession;
import com.pamarin.oauth.client.resolver.OAuthAccessTokenResolver;
import com.pamarin.oauth.client.resolver.OAuthAccessTokenResolverImpl;

/**
 *
 * @author jitta
 */
public class OAuthWebClientFilter implements ExchangeFilterFunction {

    private final OAuthAccessTokenResolver accessTokenResolver;

    public OAuthWebClientFilter() {
        this.accessTokenResolver = new OAuthAccessTokenResolverImpl();
    }

    @Override
    public Mono<ClientResponse> filter(final ClientRequest request, final ExchangeFunction exFunction) {
        return Mono.subscriberContext()
                .flatMap(context -> {
                    return extractToken(context)
                            .flatMap(sessionToken -> {
                                return exFunction.exchange(
                                        ClientRequest.from(request)
                                                .header("Authorization", "bearer " + sessionToken)
                                                .build()
                                );
                            })
                            .switchIfEmpty(exFunction.exchange(request));
                });
    }

    private Mono<String> extractToken(final Context context) {
        try {
            final ServerWebExchange exchange = context.get(ServerWebExchange.class);
            final OAuthSession session = exchange.getAttribute(OAUTH_SESSION_CONTEXT);
            if (session == null) {
                return accessTokenResolver.resolve(exchange);
            }
            return Mono.just(session.getSessionToken());
        } catch (NoSuchElementException e) {
            return Mono.empty();
        }
    }
}
