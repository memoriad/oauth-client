/*
 * Copyright 2017-Current Pamarin.com
 */
package com.pamarin.oauth.client.autoconfigure;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import static org.springframework.util.StringUtils.hasText;

/**
 *
 * @author jitta
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "pamarin.oauth.client")
public class OAuthClientProperties {

    private String id;

    private String secret;

    private String scope;

    private String authorizeSuccessUrl;

    private AuthorizationServer authorizationServer;

    private Cache cache;

    public Cache getCache() {
        if (cache == null) {
            cache = new Cache();
        }
        return cache;
    }

    public AuthorizationServer getAuthorizationServer() {
        if (authorizationServer == null) {
            authorizationServer = new AuthorizationServer();
        }
        return authorizationServer;
    }

    @Getter
    @Setter
    public static class AuthorizationServer {

        private String url;

        private String internalUrl;

        public String getInternalUrl() {
            if (!hasText(internalUrl)) {
                return url;
            }
            return internalUrl;
        }

    }

    @Getter
    @Setter
    public static class Cache {

        private Long sessionTimeout;

        public Long getSessionTimeout() {
            if (sessionTimeout == null) {
                sessionTimeout = 0L;
            }
            return sessionTimeout;
        }

    }
}
