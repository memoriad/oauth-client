/*
 * Copyright 2017-2019 Pamarin
 */
package com.pamarin.oauth.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author jitta
 */
@Getter
@Setter 
public class OAuthAccessToken { 

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("expries_in")
    private long expiresIn;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("session_token")
    private String sessionToken; 
}
