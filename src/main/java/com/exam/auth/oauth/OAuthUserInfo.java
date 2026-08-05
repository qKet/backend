package com.exam.auth.oauth;

// provider(구글/카카오/네이버) 유저정보 API 응답을 정규화한 형태
public record OAuthUserInfo(String provider, String providerUserId, String email, String name) {
}
