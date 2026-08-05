package com.exam.auth.oauth;

import com.exam.common.exception.BusinessException;
import com.exam.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Component("naver")
public class NaverOAuthClient implements SocialOAuthClient {

    private static final Logger log = LoggerFactory.getLogger(NaverOAuthClient.class);

    private static final String AUTHORIZE_URI = "https://nid.naver.com/oauth2.0/authorize";
    private static final String TOKEN_URI = "https://nid.naver.com/oauth2.0/token";
    private static final String USERINFO_URI = "https://openapi.naver.com/v1/nid/me";

    private final RestClient restClient = RestClient.create();

    @Value("${oauth.naver.client-id}")
    private String clientId;

    @Value("${oauth.naver.client-secret}")
    private String clientSecret;

    @Override
    public String buildAuthorizeUrl(String state, String redirectUri) {
        return UriComponentsBuilder.fromHttpUrl(AUTHORIZE_URI)
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", state)
                .encode()
                .build()
                .toUriString();
    }

    @Override
    public String exchangeCodeForToken(String code, String redirectUri) {
        String uri = UriComponentsBuilder.fromHttpUrl(TOKEN_URI)
                .queryParam("grant_type", "authorization_code")
                .queryParam("client_id", clientId)
                .queryParam("client_secret", clientSecret)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("code", code)
                .encode()
                .build()
                .toUriString();

        try {
            Map<String, Object> body = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            return String.valueOf(body.get("access_token"));
        } catch (Exception e) {
            log.error("네이버 토큰 교환 실패", e);
            throw new BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public OAuthUserInfo fetchUserInfo(String accessToken) {
        try {
            Map<String, Object> body = restClient.get()
                    .uri(USERINFO_URI)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            Map<String, Object> response = (Map<String, Object>) body.get("response");
            if (response == null) {
                throw new BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR);
            }
            String id = String.valueOf(response.get("id"));
            String email = (String) response.get("email");
            String name = (String) response.get("name");
            return new OAuthUserInfo("naver", id, email, name);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("네이버 사용자 정보 조회 실패", e);
            throw new BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR);
        }
    }
}
