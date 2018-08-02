package com.walkabout.corebe;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.http.AccessTokenRequiredException;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.resource.UserRedirectRequiredException;
import org.springframework.security.oauth2.client.token.AccessTokenProvider;
import org.springframework.security.oauth2.client.token.AccessTokenProviderChain;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.implicit.ImplicitAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordAccessTokenProvider;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

import java.util.Arrays;

public class OAuth2FeignRequestInterceptor implements RequestInterceptor {

    public static final String BEARER = "Bearer";

    public static final String AUTHORIZATION = "Authorization";

    private final OAuth2ClientContext oAuth2ClientContext;

    private final OAuth2ProtectedResourceDetails resource;

    private final String tokenType;

    private final String header;

    private AccessTokenProvider accessTokenProvider = new AccessTokenProviderChain(Arrays
            .<AccessTokenProvider>asList(new AuthorizationCodeAccessTokenProvider(),
                    new ImplicitAccessTokenProvider(),
                    new ResourceOwnerPasswordAccessTokenProvider(),
                    new FeignClientAccessTokenProvider()));

    /**
     * Default constructor which uses the provided OAuth2ClientContext and Bearer tokens
     * within Authorization header
     *
     * @param oAuth2ClientContext provided context
     * @param resource            type of resource to be accessed
     */
    public OAuth2FeignRequestInterceptor(OAuth2ClientContext oAuth2ClientContext,
                                         OAuth2ProtectedResourceDetails resource) {
        this(oAuth2ClientContext, resource, BEARER, AUTHORIZATION);
    }

    /**
     * Fully customizable constructor for changing token type and header name, in cases of
     * Bearer and Authorization is not the default such as "bearer", "authorization"
     *
     * @param oAuth2ClientContext current oAuth2 Context
     * @param resource            type of resource to be accessed
     * @param tokenType           type of token e.g. "token", "Bearer"
     * @param header              name of the header e.g. "Authorization", "authorization"
     */
    public OAuth2FeignRequestInterceptor(OAuth2ClientContext oAuth2ClientContext,
                                         OAuth2ProtectedResourceDetails resource, String tokenType, String header) {
        this.oAuth2ClientContext = oAuth2ClientContext;
        this.resource = resource;
        this.tokenType = tokenType;
        this.header = header;
    }

    /**
     * Create a template with the header of provided name and extracted extract
     *
     * @see RequestInterceptor#apply(RequestTemplate)
     */
    @Override
    public void apply(RequestTemplate template) {
        template.header(this.header, extract(this.tokenType));
    }

    /**
     * Extracts the token extract id the access token exists or returning an empty extract
     * if there is no one on the context it may occasionally causes Unauthorized response
     * since the token extract is empty
     *
     * @param tokenType type name of token
     * @return token value from context if it exists otherwise empty String
     */
    protected String extract(String tokenType) {
        OAuth2AccessToken accessToken = getToken();
        return String.format("%s %s", tokenType, accessToken.getValue());
    }

    /**
     * Extract the access token within the request or try to acquire a new one by
     * delegating it to {@link #acquireAccessToken()}
     *
     * @return valid token
     */
    public OAuth2AccessToken getToken() {

        OAuth2AccessToken accessToken = this.oAuth2ClientContext.getAccessToken();
        if (accessToken == null || accessToken.isExpired()) {
            try {
                accessToken = acquireAccessToken();
            } catch (UserRedirectRequiredException e) {
                this.oAuth2ClientContext.setAccessToken(null);
                String stateKey = e.getStateKey();
                if (stateKey != null) {
                    Object stateToPreserve = e.getStateToPreserve();
                    if (stateToPreserve == null) {
                        stateToPreserve = "NONE";
                    }
                    this.oAuth2ClientContext.setPreservedState(stateKey, stateToPreserve);
                }
                throw e;
            }
        }
        return accessToken;
    }

    /**
     * Try to acquire the token using a access token provider
     *
     * @return valid access token
     * @throws UserRedirectRequiredException in case the user needs to be redirected to an
     *                                       approval page or login page
     */
    protected OAuth2AccessToken acquireAccessToken()
            throws UserRedirectRequiredException {
        AccessTokenRequest tokenRequest = this.oAuth2ClientContext.getAccessTokenRequest();
        if (tokenRequest == null) {
            throw new AccessTokenRequiredException(
                    "Cannot find valid context on request for resource '"
                            + this.resource.getId() + "'.",
                    this.resource);
        }
        String stateKey = tokenRequest.getStateKey();
        if (stateKey != null) {
            tokenRequest.setPreservedState(
                    this.oAuth2ClientContext.removePreservedState(stateKey));
        }
        OAuth2AccessToken existingToken = this.oAuth2ClientContext.getAccessToken();
        if (existingToken != null) {
            this.oAuth2ClientContext.setAccessToken(existingToken);
        }
        OAuth2AccessToken obtainableAccessToken;
        obtainableAccessToken = this.accessTokenProvider.obtainAccessToken(this.resource,
                tokenRequest);
        if (obtainableAccessToken == null || obtainableAccessToken.getValue() == null) {
            throw new IllegalStateException(
                    " Access token provider returned a null token, which is illegal according to the contract.");
        }
        this.oAuth2ClientContext.setAccessToken(obtainableAccessToken);
        return obtainableAccessToken;
    }
}
