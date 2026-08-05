package com.exam.auth.oauth;

// provider별 OAuth2 authorization code flow 구현체가 따라야 하는 계약
// 각 구현체(GoogleOAuthClient/KakaoOAuthClient/NaverOAuthClient)는 provider별 엔드포인트/응답 파싱만 다름
public interface SocialOAuthClient {
    String buildAuthorizeUrl(String state, String redirectUri);
    String exchangeCodeForToken(String code, String redirectUri);
    OAuthUserInfo fetchUserInfo(String accessToken);
}
