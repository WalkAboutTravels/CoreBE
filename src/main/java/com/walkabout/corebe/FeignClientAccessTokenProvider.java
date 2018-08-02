package com.walkabout.corebe;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.resource.OAuth2AccessDeniedException;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsAccessTokenProvider;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.util.MultiValueMap;

public class FeignClientAccessTokenProvider extends ClientCredentialsAccessTokenProvider {

    private ObjectMapper mapper = new ObjectMapper();

    @Override
    protected OAuth2AccessToken retrieveToken(AccessTokenRequest request, OAuth2ProtectedResourceDetails resource, MultiValueMap<String, String> form, HttpHeaders headers) throws OAuth2AccessDeniedException {
        OAuth2AccessToken token = super.retrieveToken(request, resource, form, headers);

        if (token != null && token.getValue() == null && token.getAdditionalInformation() != null) {
            if (token.getAdditionalInformation().containsKey("data")) {
                token = this.mapper.convertValue(token.getAdditionalInformation().get("data"), OAuth2AccessToken.class);
            }
        }

        return token;
    }
}
