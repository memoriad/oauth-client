/*
 * Copyright 2017-Current Pamarin.com
 */
package com.pamarin.oauth.client.autoconfigure;

import com.pamarin.core.commons.autoconfigure.CoreCommonsProperties;
import com.pamarin.core.commons.cache.MemoryCacheRepository;
import com.pamarin.core.commons.provider.ServerWebExchangeProvider;
import com.pamarin.core.commons.security.ClassPathDERFileRSAKeyPairsAdapter;
import com.pamarin.core.commons.security.RSAKeyPairs;
import com.pamarin.core.commons.security.RSAPrivateKeyReader;
import com.pamarin.core.commons.security.RSAPublicKeyReader;
import com.pamarin.core.error.handler.WebClientExceptionTranslator;
import com.pamarin.oauth.client.OAuthClientOperations;
import com.pamarin.oauth.client.OAuthClientOperationsImpl;
import com.pamarin.oauth.client.SessionCacheOAuthClientOperations;
import com.pamarin.oauth.client.filter.OAuthWebClientFilter;
import com.pamarin.oauth.client.security.OAuthClientServerSecurityContextRepository;
import com.pamarin.oauth.client.security.OAuthSessionContext;
import com.pamarin.oauth.client.security.OAuthSessionContextImpl;
import com.pamarin.oauth.client.security.OAuthSessionTokenDecoder;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.time.Duration;
import javax.annotation.PostConstruct;
import javax.net.ssl.SSLException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;

/**
 *
 * @author jitta
 */
@Slf4j
@Configuration
@AutoConfigureAfter({WebFluxAutoConfiguration.class})
@EnableConfigurationProperties({OAuthClientProperties.class})
public class OAuthClientAutoConfiguration {

    private final OAuthClientProperties properties;

    @Autowired
    public OAuthClientAutoConfiguration(OAuthClientProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean({ServerSecurityContextRepository.class})
    public ServerSecurityContextRepository serverSecurityContextRepository() {
        return new OAuthClientServerSecurityContextRepository();
    }

    @Bean("sessionTokenKeyPairs")
    public RSAKeyPairs sessionTokenKeyPairs(RSAPrivateKeyReader privateKeyReader, RSAPublicKeyReader publicKeyReader) {
        return new ClassPathDERFileRSAKeyPairsAdapter(privateKeyReader, publicKeyReader) {

            @Override
            protected String getPrivateKeyPath() {
                return null;
            }

            @Override
            protected String getPublicKeyPath() {
                return "/key/public-key.der";
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean({ClientHttpConnector.class})
    public ClientHttpConnector clientHttpConnector() {
        final int CONNECT_TIMEOUT_MILLIS = 1000 * 5;
        log.debug("TcpClient CONNECT_TIMEOUT_MILLIS => {}", CONNECT_TIMEOUT_MILLIS);
        final TcpClient tcpClient = TcpClient.newConnection()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MILLIS);
        return new ReactorClientHttpConnector(HttpClient.from(tcpClient));
    }

    @Primary
    @Bean("oauthWebClient")
    public WebClient oauthWebClient() throws SSLException {

        final SslContext sslContext = SslContextBuilder
                .forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();

        final HttpClient httpClient = HttpClient
                .create()
                .secure(sslContextSpec -> sslContextSpec.sslContext(sslContext));

        final ClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);

        return WebClient.builder()
                .clientConnector(connector)
                .filter(new OAuthWebClientFilter())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean({OAuthSessionContext.class})
    public OAuthSessionContext oauthSessionContext(ServerWebExchangeProvider exchangeProvider) {
        return new OAuthSessionContextImpl(exchangeProvider);
    }

    @Bean
    @ConditionalOnMissingBean({OAuthClientOperations.class})
    public OAuthClientOperations oauthClientOperations(
            OAuthClientProperties clientProperties,
            CoreCommonsProperties commonsProperties,
            OAuthSessionTokenDecoder sessionTokenDecoder,
            WebClientExceptionTranslator exceptionTranslator,
            MemoryCacheRepository memoryCacheRepository
    ) {
        final OAuthClientOperations clientOperations = new OAuthClientOperationsImpl(
                clientProperties,
                commonsProperties,
                sessionTokenDecoder,
                exceptionTranslator
        );

        if (properties.getCache().getSessionTimeout() > 0) {
            log.debug("enabled cache oauth session...");
            return new SessionCacheOAuthClientOperations(
                    clientOperations,
                    memoryCacheRepository,
                    Duration.ofMillis(properties.getCache().getSessionTimeout())
            );
        }

        return clientOperations;
    }

    @PostConstruct
    public void showConfig() {
        log.debug("OAuth Client Configuration");
        log.debug("**********************************************************");
        log.debug("pamarin.oauth.client.authorization-server.url => {}", properties.getAuthorizationServer().getUrl());
        log.debug("pamarin.oauth.client.authorization-server.internal-url => {}", properties.getAuthorizationServer().getInternalUrl());
        log.debug("pamarin.oauth.client.id => {}", properties.getId());
        log.debug("pamarin.oauth.client.secret => ****");
        log.debug("pamarin.oauth.client.scope => {}", properties.getScope());
        log.debug("pamarin.oauth.client.authorize-success-url => {}", properties.getAuthorizeSuccessUrl());
        log.debug("pamarin.oauth.client.cache.session-timeout => {}", properties.getCache().getSessionTimeout());
        log.debug("**********************************************************");
    }
}
