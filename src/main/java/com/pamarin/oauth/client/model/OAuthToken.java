/*
 * Copyright 2017-Current Pamarin.com
 */
package com.pamarin.oauth.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @author jitta
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthToken {

    @JsonProperty("access_token")
    private String accessToken;

}
