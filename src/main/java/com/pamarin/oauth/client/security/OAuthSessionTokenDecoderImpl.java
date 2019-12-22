/*
 * Copyright 2017-Current Pamarin.com
 */
package com.pamarin.oauth.client.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pamarin.core.commons.exception.InvalidTokenException;
import com.pamarin.core.commons.util.Base64Utils;
import com.pamarin.oauth.client.model.OAuthSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import static org.springframework.util.StringUtils.hasText;
import reactor.core.publisher.Mono;

/**
 *
 * @author jitta
 */
@Slf4j
@Component
public class OAuthSessionTokenDecoderImpl implements OAuthSessionTokenDecoder {

    private final ObjectMapper objectMapper;

    @Autowired
    public OAuthSessionTokenDecoderImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<OAuthSession> decode(final String sessionToken) {
        if (!hasText(sessionToken)) {
            return Mono.error(new InvalidTokenException("require sessionToken"));
        }
        final String[] arr = StringUtils.split(sessionToken, ".");
        if (arr.length != 3) {
            return Mono.error(new InvalidTokenException("invalid token"));
        }
        return Mono.fromCallable(() -> Base64Utils.decode(arr[1]))
                .flatMap(json -> convert(sessionToken, json));

    }

    private List<String> convertToList(final JsonNode node) {
        final List<String> elements = new ArrayList<>();
        final Iterator<JsonNode> iterator = node.elements();
        while (iterator.hasNext()) {
            JsonNode element = iterator.next();
            if (element != null) {
                String text = element.asText();
                if (hasText(text)) {
                    elements.add(text);
                }
            }
        }
        return elements;
    }

    private Mono<OAuthSession> convert(final String sessionToken, final String json) {
        try {
            final JsonNode node = objectMapper.readTree(json);
            final JsonNode nameNode = node.get("user_name");
            final JsonNode photoUrlNode = node.get("user_photo_url");
            final JsonNode usernameNode = node.get("user_username");
            final JsonNode lastAccessedAtNode = node.get("lastAccessedAt");
            final JsonNode loggedinAtNode = node.get("loggedinAt");
            final JsonNode confirmPasswordAtNode = node.get("confirmPasswordAt");
            return Mono.just(
                    OAuthSession.builder()
                            .sessionToken(sessionToken)
                            .id(node.get("id").asText())
                            .issuedAt(node.get("issuedAt").asLong())
                            .expiresAt(node.get("expiresAt").asLong())
                            .lastAccessedAt(lastAccessedAtNode == null ? 0L : lastAccessedAtNode.asLong())
                            .loggedinAt(loggedinAtNode == null ? 0L : loggedinAtNode.asLong())
                            .confirmPasswordAt(confirmPasswordAtNode == null ? 0L : confirmPasswordAtNode.asLong())
                            .user(
                                    OAuthSession.User.builder()
                                            .id(node.get("user_id").asText())
                                            .name(nameNode == null ? null : nameNode.asText())
                                            .photoUrl(photoUrlNode == null ? null : photoUrlNode.asText())
                                            .username(usernameNode == null ? null : usernameNode.asText())
                                            .authorities(convertToList(node.get("user_authorities")))
                                            .build()
                            )
                            .client(
                                    OAuthSession.Client.builder()
                                            .id(node.get("client_id").asText())
                                            .name(node.get("client_name").asText())
                                            .scopes(convertToList(node.get("client_scopes")))
                                            .build()
                            )
                            .build()
            );
        } catch (IOException e) {
            return Mono.error(new InvalidTokenException("invalid token", e));
        }
    }
}
