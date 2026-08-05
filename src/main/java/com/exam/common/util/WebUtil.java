package com.exam.common.util;

import jakarta.servlet.http.HttpServletRequest;

// ins_ip / upt_ip 감사 컬럼에 넣을 클라이언트 IP를 구하는 공용 유틸
public class WebUtil {

    private WebUtil() {}

    // AWS ALB 뒤에서 돌기 때문에 request.getRemoteAddr()는 ALB의 내부 IP가 잡힘
    // 실제 클라이언트 IP는 ALB가 X-Forwarded-For 헤더에 넣어서 넘겨줌 (여러 프록시를 거치면 콤마로 나열, 맨 앞이 최초 클라이언트)
    public static String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
