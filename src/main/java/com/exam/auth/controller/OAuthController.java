package com.exam.auth.controller;

import com.exam.auth.dto.UserDTO;
import com.exam.auth.oauth.OAuthUserInfo;
import com.exam.auth.oauth.SocialOAuthClient;
import com.exam.auth.service.OAuthService;
import com.exam.common.exception.BusinessException;
import com.exam.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

// 소셜 로그인(Google/Kakao/Naver) — 기존 세션 기반 인증(UserController)과 동일한 방식으로
// authorization code flow를 직접 처리하고 성공 시 session.setAttribute("loginUser", ...)로 로그인시킴.
// 브라우저 최상위 리다이렉트로 진행되는 흐름이라, GlobalExceptionHandler의 JSON 에러 응답 대신
// /login?oauthError=... 로 리다이렉트시켜 프론트에서 메시지를 보여준다.
@RestController
@RequestMapping("/oauth2")
public class OAuthController {

    private static final Logger log = LoggerFactory.getLogger(OAuthController.class);

    private final Map<String, SocialOAuthClient> clients;
    private final OAuthService oAuthService;

    @Value("${app.base-url}")
    private String baseUrl;

    public OAuthController(Map<String, SocialOAuthClient> clients, OAuthService oAuthService) {
        this.clients = clients;
        this.oAuthService = oAuthService;
    }

    /***********************************
     *  URL      :  "/oauth2/authorize/{provider}"
     *  이름      :   소셜 로그인 시작
     *  기능      :   provider의 OAuth 동의 화면으로 리다이렉트
     *  method   :   GET
     ************************************/
    @GetMapping("/authorize/{provider}")
    public ResponseEntity<Void> authorize(@PathVariable String provider, HttpSession session) {
        SocialOAuthClient client = requireClient(provider);
        String state = UUID.randomUUID().toString();
        session.setAttribute("oauthState", state);

        String authorizeUrl = client.buildAuthorizeUrl(state, redirectUri(provider));
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(authorizeUrl))
                .build();
    }

    /***********************************
     *  URL      :  "/oauth2/callback/{provider}"
     *  이름      :   소셜 로그인 콜백
     *  기능      :   authorization code를 access token으로 교환 후 유저정보 조회, 로그인/회원가입 처리
     *  method   :   GET
     ************************************/
    @GetMapping("/callback/{provider}")
    public ResponseEntity<Void> callback(
            @PathVariable String provider,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            HttpSession session,
            HttpServletRequest request
    ) {
        if (error != null) {
            return redirectToLogin("CANCELLED");
        }

        try {
            SocialOAuthClient client = requireClient(provider);

            Object savedState = session.getAttribute("oauthState");
            session.removeAttribute("oauthState");
            if (savedState == null || !savedState.equals(state)) {
                throw new BusinessException(ErrorCode.INVALID_OAUTH_STATE);
            }

            String accessToken = client.exchangeCodeForToken(code, redirectUri(provider));
            OAuthUserInfo userInfo = client.fetchUserInfo(accessToken);

            UserDTO user = oAuthService.loginOrRegister(userInfo, request);
            session.setAttribute("loginUser", user);

            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(baseUrl + "/"))
                    .build();
        } catch (BusinessException e) {
            return redirectToLogin(e.getErrorCode().getCode());
        } catch (Exception e) {
            log.error("소셜 로그인 콜백 처리 실패 (provider={})", provider, e);
            return redirectToLogin(ErrorCode.OAUTH_PROVIDER_ERROR.getCode());
        }
    }

    private SocialOAuthClient requireClient(String provider) {
        SocialOAuthClient client = clients.get(provider);
        if (client == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "지원하지 않는 로그인 방식입니다.");
        }
        return client;
    }

    private String redirectUri(String provider) {
        return baseUrl + "/api/oauth2/callback/" + provider;
    }

    private ResponseEntity<Void> redirectToLogin(String errorCode) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(baseUrl + "/login?oauthError=" + errorCode))
                .build();
    }
}
