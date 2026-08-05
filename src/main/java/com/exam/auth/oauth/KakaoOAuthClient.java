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

@Component("kakao")
public class KakaoOAuthClient implements SocialOAuthClient {

    private static final Logger log = LoggerFactory.getLogger(KakaoOAuthClient.class);

    private static final String AUTHORIZE_URI = "https://kauth.kakao.com/oauth/authorize";
    private static final String TOKEN_URI = "https://kauth.kakao.com/oauth/token";
    private static final String USERINFO_URI = "https://kapi.kakao.com/v2/user/me";

    private final RestClient restClient = RestClient.create();

    @Value("${oauth.kakao.client-id}")
    private String clientId;

    @Value("${oauth.kakao.client-secret}")
    private String clientSecret;

    @Override
    public String buildAuthorizeUrl(String state, String redirectUri) {
        return UriComponentsBuilder.fromHttpUrl(AUTHORIZE_URI)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("state", state)
                .encode()
                .build()
                .toUriString();
    }

    @Override
    public String exchangeCodeForToken(String code, String redirectUri) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        if (clientSecret != null && !clientSecret.isBlank()) {
            form.add("client_secret", clientSecret);
        }
        form.add("redirect_uri", redirectUri);
        form.add("code", code);

        try {
            Map<String, Object> body = restClient.post()
                    .uri(TOKEN_URI)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            return String.valueOf(body.get("access_token"));
        } catch (Exception e) {
            log.error("카카오 토큰 교환 실패", e);
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
            String id = String.valueOf(body.get("id"));
            Map<String, Object> kakaoAccount = (Map<String, Object>) body.get("kakao_account");
            String email = null;
            String name = null;
            if (kakaoAccount != null) {
                email = (String) kakaoAccount.get("email");
                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                if (profile != null) {
                    name = (String) profile.get("nickname");
                }
            }
            return new OAuthUserInfo("kakao", id, email, name);
        } catch (Exception e) {
            log.error("카카오 사용자 정보 조회 실패", e);
            throw new BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR);
        }
    }
}
