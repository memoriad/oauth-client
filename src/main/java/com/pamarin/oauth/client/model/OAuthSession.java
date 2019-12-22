/*
 * Copyright 2017-Current Pamarin.com
 */
package com.pamarin.oauth.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 *
 * @author jitta
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class OAuthSession implements Serializable {

    @JsonProperty("session_token")
    private String sessionToken;

    private String id;

    private long issuedAt;

    private long expiresAt;

    private long lastAccessedAt;

    private long loggedinAt;

    private long confirmPasswordAt;

    private User user;

    private Client client;

    public User getUser() {
        if (user == null) {
            user = new User();
        }
        return user;
    }

    public Client getClient() {
        if (client == null) {
            client = new Client();
        }
        return client;
    }

    @Getter
    @Setter
    @Builder
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(of = "id")
    public static class User implements Serializable {

        private static final long serialVersionUID = 1L;

        private String id;

        private String username;

        private String name;
        
        private String photoUrl;

        private List<String> authorities;

        public List<String> getAuthorities() {
            if (authorities == null) {
                authorities = new ArrayList<>();
            }
            return authorities;
        }
    }

    @Getter
    @Setter
    @Builder
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(of = "id")
    public static class Client implements Serializable {

        private static final long serialVersionUID = 1L;

        private String id;

        private String name;

        private List<String> scopes;

        public List<String> getScopes() {
            if (scopes == null) {
                scopes = new ArrayList<>();
            }
            return scopes;
        }

    }

}
