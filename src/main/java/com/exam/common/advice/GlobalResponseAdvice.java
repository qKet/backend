package com.exam.common.advice;

import com.exam.common.dto.ApiResponse;
import com.exam.common.exception.ErrorResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

// 컨트롤러가 리턴한 값을 JSON으로 내보내기 직전에 가로채서 공통 필드를 자동으로 붙여줌.
// 새로 만드는 컨트롤러는 그냥 데이터(DTO, List 등)만 리턴하면 { success, message, data, timestamp } 로
// 알아서 감싸주므로, 매 엔드포인트마다 손으로 ApiResponse.success(...) 를 호출할 필요가 없음.
//
// 이미 Map으로 success/message 를 직접 만들어 리턴하는 기존 컨트롤러들(UserController, AdminController 등)은
// success/user/message 같은 필드를 그대로 두고 timestamp 필드만 추가해줌 — 이중 포장은 하지 않되,
// 컨트롤러 코드를 하나도 안 고쳐도 모든 응답에 timestamp가 붙게 하기 위함.
//
// basePackages="com.exam" 로 범위를 한정한 이유: Actuator(health, prometheus)는 K8s 헬스체크/모니터링이
// 기대하는 응답 형식이 따로 있어서, 여길 건드리면 배포 환경에서 헬스체크가 깨질 수 있음.
@ControllerAdvice(basePackages = "com.exam")
public class GlobalResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof ApiResponse || body instanceof ErrorResponse || body instanceof String) {
            // ErrorResponse: GlobalExceptionHandler가 이미 만든 실패 응답 — 성공으로 다시 감싸면 안 됨
            // String: StringHttpMessageConverter 가 처리하는 응답이라 여기서 감싸면 타입 에러가 남
            return body;
        }
        if (body instanceof Map<?, ?> map) {
            // 기존 컨트롤러들이 new HashMap<>()을 쓰고 있어서 원래 필드 순서가 보장이 안 됨
            // (해시 버킷 순서라 매번 달라질 수 있음) → success, timestamp를 맨 앞에 고정해서 순서를 통일
            Map<Object, Object> ordered = new LinkedHashMap<>();
            if (map.containsKey("success")) {
                ordered.put("success", map.get("success"));
            }
            ordered.put("timestamp", map.containsKey("timestamp") ? map.get("timestamp") : LocalDateTime.now());
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!"success".equals(entry.getKey()) && !"timestamp".equals(entry.getKey())) {
                    ordered.put(entry.getKey(), entry.getValue());
                }
            }
            return ordered;
        }
        return ApiResponse.success(body);
    }
}
