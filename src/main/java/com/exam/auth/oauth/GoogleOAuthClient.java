package com.exam.auth.oauth;

import com.exam.common.exception.BusinessException;
import com.exam.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Component("google")
public class GoogleOAuthClient implements SocialOAuthClient {

    private static final Logger log = LoggerFactory.getLogger(GoogleOAuthClient.class);

    private static final String AUTHORIZE_URI = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URI = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_URI = "https://www.googleapis.com/oauth2/v3/userinfo";
    private static final String SCOPE = "openid email profile";

    private final RestClient restClient = RestClient.create();

    @Value("${oauth.google.client-id}")
    private String clientId;

    @Value("${oauth.google.client-secret}")
    private String clientSecret;

    @Override
    public String buildAuthorizeUrl(String state, String redirectUri) {
        return UriComponentsBuilder.fromHttpUrl(AUTHORIZE_URI)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", SCOPE)
                .queryParam("state", state)
                .encode()
                .build()
                .toUriString();
    }

    @Override
    public String exchangeCodeForToken(String code, String redirectUri) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("code", code);
        form.add("redirect_uri", redirectUri);
        form.add("grant_type", "authorization_code");

        try {
            Map<String, Object> body = restClient.post()
                    .uri(TOKEN_URI)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            return String.valueOf(body.get("access_token"));
        } catch (Exception e) {
            log.error("구글 토큰 교환 실패", e);
            throw new BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR);
        }
    }

    @Override
    public OAuthUserInfo fetchUserInfo(String accessToken) {
        try {
            Map<String, Object> body = restClient.get()
                    .uri(USERINFO_URI)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            String sub = String.valueOf(body.get("sub"));
            String email = (String) body.get("email");
            String name = (String) body.get("name");
            return new OAuthUserInfo("google", sub, email, name);
        } catch (Exception e) {
            log.error("구글 사용자 정보 조회 실패", e);
            throw new BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR);
        }
    }
}
