/*
 * Copyright 2017-Current Pamarin.com
 */
package com.pamarin.oauth.client.filter;

import com.pamarin.core.commons.autoconfigure.CoreCommonsProperties;
import com.pamarin.core.commons.exception.InvalidHttpAuthorizationException;
import static com.pamarin.core.commons.util.BrowserUtils.isBrowser;
import com.pamarin.core.commons.util.DefaultHttpAuthorizationParser;
import com.pamarin.core.commons.util.DefaultHttpAuthorizeBearerParser;
import com.pamarin.core.commons.util.HttpAuthorizeBearerParser;
import com.pamarin.core.commons.util.HttpUtils;
import com.pamarin.core.commons.util.QuerystringBuilder;
import com.pamarin.core.error.model.ErrorResponse;
import com.pamarin.oauth.client.OAuthAccessTokenOperations;
import com.pamarin.oauth.client.OAuthLoginSession;
import com.pamarin.oauth.client.autoconfigure.OAuthClientProperties;
import com.pamarin.core.error.exception.ErrorResponseException;
import com.pamarin.oauth.client.exception.OAuthAccessDeniedException;
import com.pamarin.oauth.client.exception.OAuthAuthenticationException;
import com.pamarin.oauth.client.exception.OAuthAuthorizationException;
import com.pamarin.oauth.client.resolver.OAuthAccessTokenResolver;
import com.pamarin.oauth.client.resolver.OAuthRefreshTokenResolver;
import com.pamarin.oauth.client.security.OAuthAuthorizationState;
import com.pamarin.oauth.client.util.OAuthClientUtils;
import static java.lang.String.format;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import static org.springframework.util.StringUtils.hasText;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import com.pamarin.core.commons.security.Base64AESEncryption;
import com.pamarin.core.commons.security.Cors;
import com.pamarin.core.commons.security.DefaultAESEncryption;
import com.pamarin.core.commons.security.DefaultBase64AESEncryption;
import com.pamarin.oauth.client.exception.OAuthAccessDenied401Exception;

/**
 *
 * @author jitta
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class OAuthClientSessionWebFilter implements WebFilter {

    private static final String ANONYMOUS_TOKEN = "ANONYMOUS";

    private static final String CONTINUE_URI_COOKIE = "continue_uri";

    private final Cors cors;

    private final OAuthLoginSession loginSession;

    private final OAuthClientProperties clientProperties;

    private final CoreCommonsProperties commonsProperties;

    private final Base64AESEncryption base64AESEncryption;

    private final OAuthAuthorizationState authorizationState;

    private final OAuthAccessTokenResolver accessTokenResolver;

    private final OAuthRefreshTokenResolver refreshTokenResolver;

    private final OAuthAccessTokenOperations accessTokenOperations;

    private final HttpAuthorizeBearerParser httpAuthorizeBearerParser;

    @Autowired
    public OAuthClientSessionWebFilter(
            final Cors cors,
            final OAuthLoginSession loginSession,
            final OAuthClientProperties clientProperties,
            final CoreCommonsProperties commonsProperties,
            final OAuthAuthorizationState authorizationState,
            final OAuthAccessTokenResolver accessTokenResolver,
            final OAuthRefreshTokenResolver refreshTokenResolver,
            final OAuthAccessTokenOperations accessTokenOperations
    ) {
        this.loginSession = loginSession;
        this.cors = cors;
        this.clientProperties = clientProperties;
        this.commonsProperties = commonsProperties;
        this.authorizationState = authorizationState;
        this.accessTokenResolver = accessTokenResolver;
        this.refreshTokenResolver = refreshTokenResolver;
        this.accessTokenOperations = accessTokenOperations;
        this.base64AESEncryption = new DefaultBase64AESEncryption(DefaultAESEncryption.withKeyLength16());
        this.httpAuthorizeBearerParser = new DefaultHttpAuthorizeBearerParser(new DefaultHttpAuthorizationParser());
    }

    private boolean isCorsRequest(final ServerWebExchange exchange) {
        return exchange.getRequest().getMethod() == HttpMethod.OPTIONS
                || cors.matchesUrl(exchange.getRequest().getHeaders().getOrigin());
    }

    private boolean isApiRequest(final ServerWebExchange exchange) {
        return exchange.getRequest().getPath().value().startsWith("/api/");
    }

    private boolean isStaticResource(final ServerWebExchange exchange) {
        final String path = exchange.getRequest().getPath().value();
        return path.startsWith("/fonts/")
                || path.startsWith("/js/")
                || path.startsWith("/css/")
                || path.startsWith("/assets/")
                || path.startsWith("/public/")
                || path.startsWith("/static/")
                || path.startsWith("/resources/")
                || path.endsWith(".js")
                || path.endsWith(".css")
                || path.endsWith(".ico")
                || path.endsWith(".ttf")
                || path.endsWith(".woff")
                || path.endsWith(".woff2");
    }

    @Override
    public Mono<Void> filter(final ServerWebExchange exchange, final WebFilterChain chain) {

        if (isStaticResource(exchange)) {
            return chain.filter(exchange);
        }

        try {
            return loginSession.login(getBearerToken(exchange), exchange)
                    .then(chain.filter(exchange));

        } catch (InvalidHttpAuthorizationException e) {

            if (!isBrowser(exchange) || isCorsRequest(exchange)) {
                return chain.filter(exchange);
            }

            final MultiValueMap<String, String> queryParams = exchange.getRequest().getQueryParams();
            final String code = queryParams.getFirst("code");
            final String state = queryParams.getFirst("state");
            if (hasText(code) && hasText(state)) {
                return verifyAuthorizationCode(code, exchange);
            }

            final String error = queryParams.getFirst("error");
            final String errorStatus = queryParams.getFirst("error_status");
            if (hasText(error) && hasText(errorStatus)) {
                if (hasText(state)) {
                    return authorizationState.verify(exchange)
                            .then(buildError(error, errorStatus, queryParams));
                }
                return buildError(error, errorStatus, queryParams);
            }

            return verifyAccessToken(exchange, chain);
        }
    }

    private Mono<Void> verifyAuthorizationCode(final String code, final ServerWebExchange exchange) {
        return authorizationState.verify(exchange)
                .then(
                        accessTokenOperations.getAccessTokenByAuthorizationCode(code, exchange)
                                .flatMap(accessToken -> {
                                    return Mono.fromRunnable(() -> {
                                        HttpUtils.redirectTo(
                                                exchange.getResponse(),
                                                getAuthorizeSuccessUrl(exchange)
                                        );
                                    });
                                })
                );
    }

    private Mono<Void> buildError(
            final String error,
            final String errorStatus,
            final MultiValueMap<String, String> queryParams
    ) {
        final String errorAt = queryParams.getFirst("error_at");
        return Mono.error(new ErrorResponseException(
                ErrorResponse.builder()
                        .error(error)
                        .errorStatus(!hasText(errorStatus) ? 500 : Integer.parseInt(errorStatus))
                        .errorDescription(queryParams.getFirst("error_description"))
                        .errorAt(!hasText(errorAt) ? 0 : Long.parseLong(errorAt))
                        .errorUri(queryParams.getFirst("error_uri"))
                        .errorCode(queryParams.getFirst("error_code"))
                        .errorOn(queryParams.getFirst("error_on"))
                        .state(queryParams.getFirst("state"))
                        .build()
        ));
    }

    private Mono<Void> verifyAccessToken(final ServerWebExchange exchange, final WebFilterChain chain) {
        return accessTokenResolver.resolve(exchange)
                .defaultIfEmpty(ANONYMOUS_TOKEN)
                .flatMap(accessToken -> {
                    return loginSession.loginByAccessToken(token(accessToken), exchange)
                            .then(chain.filter(exchange))
                            .onErrorResume(OAuthAuthenticationException.class, e -> refreshToken(exchange, chain));
                });
    }

    private Mono<String> getAuthorizationUrl(final ServerWebExchange exchange) {
        return authorizationState.create(exchange)
                .map(state -> {
                    return format("%s/oauth/v1/authorize?%s",
                            clientProperties.getAuthorizationServer().getUrl(),
                            new QuerystringBuilder()
                                    .addParameter("response_type", "code")
                                    .addParameter("client_id", clientProperties.getId())
                                    .addParameter("redirect_uri", getCallbackUrl())
                                    .addParameter("scope", clientProperties.getScope())
                                    .addParameter("state", state)
                                    .build()
                    );
                });
    }

    private Mono<Void> refreshToken(final ServerWebExchange exchange, final WebFilterChain chain) {
        return refreshTokenResolver.resolve(exchange)
                .defaultIfEmpty(ANONYMOUS_TOKEN)
                .flatMap(refreshToken -> {
                    return accessTokenOperations.getAccessTokenByRefreshToken(token(refreshToken), exchange)
                            .flatMap(token -> {
                                return loginSession.loginBySessionToken(token.getSessionToken(), exchange)
                                        .then(chain.filter(exchange))
                                        .onErrorResume(OAuthAccessDeniedException.class, ex -> redirectToAuthorize(exchange, ex))
                                        .onErrorResume(OAuthAccessDenied401Exception.class, ex -> redirectToAuthorize(exchange, ex));
                            })
                            .onErrorResume(OAuthAuthorizationException.class, e -> {
                                return chain.filter(exchange)
                                        .onErrorResume(OAuthAccessDeniedException.class, ex -> redirectToAuthorize(exchange, ex))
                                        .onErrorResume(OAuthAccessDenied401Exception.class, ex -> redirectToAuthorize(exchange, ex));
                            });
                });
    }

    private String token(final String token) {
        if (ANONYMOUS_TOKEN.equals(token)) {
            return null;
        }
        return token;
    }

    private String getBearerToken(final ServerWebExchange exchange) {
        final String authorization = exchange.getRequest().getHeaders().getFirst("Authorization");
        return httpAuthorizeBearerParser.parse(authorization);
    }

    private String getCallbackUrl() {
        return commonsProperties.getApplication().getUrl() + "/oauth/callback";
    }

    private Mono<Void> redirectToAuthorize(final ServerWebExchange exchange, final Throwable e) {
        if (isApiRequest(exchange)) {
            return Mono.error(e);
        }
        return redirectToAuthorize(exchange);
    }

    private Mono<Void> redirectToAuthorize(final ServerWebExchange exchange) {
        return getAuthorizationUrl(exchange)
                .doOnNext(authorizeUrl -> {

                    if (isHttpGet(exchange)) {
                        saveContinueUrl(exchange);
                    }

                    OAuthClientUtils.clearTokenCookies(exchange);
                    HttpUtils.redirectTo(exchange.getResponse(), authorizeUrl);
                })
                .then();
    }

    private String getAuthorizeSuccessUrl(final ServerWebExchange exchange) {
        final String continueUri = getContinueUri(exchange);
        if (hasText(continueUri)) {
            return continueUri;
        }
        final String url = clientProperties.getAuthorizeSuccessUrl();
        if (!hasText(url)) {
            return commonsProperties.getApplication().getUrl();
        }
        return url;
    }

    private boolean isHttpGet(final ServerWebExchange exchange) {
        return HttpMethod.GET == exchange.getRequest().getMethod();
    }

    private String buildContinueUri(final ServerWebExchange exchange) {
        final URI uri = exchange.getRequest().getURI();
        final String continueUri = commonsProperties.getApplication().getUrl() + uri.getRawPath();
        if (hasText(uri.getRawQuery())) {
            return continueUri + "?" + uri.getRawQuery();
        }
        return continueUri;
    }

    private void saveContinueUrl(final ServerWebExchange exchange) {
        try {
            final String continueUri = buildContinueUri(exchange);
            final String encypted = base64AESEncryption.encrypt(continueUri, clientProperties.getSecret());
            final int maxAgeSeconds = 60 * 60;//1 hr
            saveContinueUriCookie(encypted, maxAgeSeconds, exchange);
        } catch (Exception e) {
            log.warn("saveContinueUrl error => {}", e.getMessage());
        }
    }

    private String getContinueUri(final ServerWebExchange exchange) {
        final HttpCookie cookie = exchange.getRequest().getCookies().getFirst(CONTINUE_URI_COOKIE);
        if (cookie == null) {
            return null;
        }
        final String value = cookie.getValue();
        if (!hasText(value)) {
            return null;
        }
        try {
            return base64AESEncryption.decrypt(value, clientProperties.getSecret());
        } catch (Exception e) {
            return null;
        } finally {
            saveContinueUriCookie("", 1, exchange);
        }
    }

    private static void saveContinueUriCookie(final String cookieValue, final int maxAge, final ServerWebExchange exchange) {
        final ServerHttpResponse httpResp = exchange.getResponse();
        httpResp.beforeCommit(() -> Mono.fromRunnable(() -> {
            httpResp.addCookie(
                    ResponseCookie.from(CONTINUE_URI_COOKIE, cookieValue)
                            .path("/")
                            .httpOnly(true)
                            .secure("https".equalsIgnoreCase(exchange.getRequest().getURI().getScheme()))
                            .sameSite("Lax")
                            .maxAge(maxAge)
                            .build()
            );
        }));
    }
}
