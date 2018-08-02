package com.walkabout.corebe;

import com.walkabout.corebe.client.SabreClient;
import feign.Feign;
import feign.Logger;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.okhttp.OkHttpClient;
import feign.slf4j.Slf4jLogger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.resource.BaseOAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;

@Configuration
public class SabreConfiguration {

    private OAuth2ClientContext oAuth2ClientContext;

    @Value("${sabre.irl-auth}")
    private String uri;

    @Value("${sabre.client-id}")
    private String clientId;

    @Value("${sabre.secret}")
    private String secret;

    @Bean
    public SabreClient sabreClient(){
        return Feign.builder()
                .client(new OkHttpClient())
                .decoder(new JacksonDecoder())
                .encoder(new JacksonEncoder())
                .logger(new Slf4jLogger(SabreClient.class))
                .logLevel(Logger.Level.FULL)
                .requestInterceptor(new OAuth2FeignRequestInterceptor(oAuth2ClientContext, resource()))
                .target(SabreClient.class, uri);

    }

    private OAuth2ProtectedResourceDetails resource() {

        BaseOAuth2ProtectedResourceDetails details = new BaseOAuth2ProtectedResourceDetails();
        details.setAccessTokenUri(uri);
        details.setClientId(clientId);
        details.setClientSecret(secret);

        return details;
    }


}
