package com.exam.review.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

// AI01_SPOIL01 — 감상평 본문을 OpenAI에 보내 스포일러 포함 여부를 자동 판별한다.
// 사용자가 직접 체크하는 UI 없이 전적으로 AI 판단에만 의존하는 "완전 자동화" 방식으로 결정했음
// (2026-08-05 사용자 확인) — 대신 AI 호출이 실패하거나 키가 비어있으면 무조건 false(스포일러 아님)로
// 폴백해서, 이 기능 하나 때문에 감상평 작성 자체가 막히는 일은 없게 한다.
@Service
public class SpoilerDetectionServiceImpl implements SpoilerDetectionService {

    private static final Logger log = LoggerFactory.getLogger(SpoilerDetectionServiceImpl.class);
    private static final String OPENAI_CHAT_API = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-4o-mini";
    private static final String SYSTEM_PROMPT =
            "너는 공연(뮤지컬/연극/콘서트) 감상평에 스포일러가 포함되어 있는지 판별하는 분류기다. " +
            "결말, 반전, 주요 줄거리 전개, 특정 장면의 구체적 내용이 담겨 있으면 스포일러다. " +
            "단순히 좋았다/별로였다는 감상, 배우 연기력, 무대 연출에 대한 일반적인 평가는 스포일러가 아니다. " +
            "다른 설명 없이 SPOILER 또는 NONE 중 한 단어로만 답하라.";

    private final RestTemplate restTemplate = new RestTemplate();
    private final String apiKey;

    public SpoilerDetectionServiceImpl(@Value("${openai.api-key}") String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public boolean isSpoiler(String content) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("OPENAI_API_KEY가 설정되지 않아 스포일러 자동판별을 건너뜁니다.");
            return false;
        }

        Map<String, Object> body = Map.of(
                "model", MODEL,
                "temperature", 0,
                "max_tokens", 5,
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", content)
                )
        );

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    OPENAI_CHAT_API,
                    new HttpEntity<>(body, buildAuthHeaders()),
                    Map.class
            );
            return "SPOILER".equalsIgnoreCase(extractAnswer(response.getBody()));
        } catch (Exception e) {
            // 네트워크 오류, 요금 소진, 응답 형식 변경 등 어떤 이유로든 판별에 실패하면
            // 감상평 작성 자체를 막지 않고 스포일러 아님으로 처리한다.
            log.warn("스포일러 자동판별 호출 실패 — 기본값(스포일러 아님)으로 처리합니다.", e);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private String extractAnswer(Map<String, Object> responseBody) {
        if (responseBody == null) {
            return null;
        }
        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        if (message == null) {
            return null;
        }
        Object answer = message.get("content");
        return answer == null ? null : answer.toString().trim();
    }

    private HttpHeaders buildAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
