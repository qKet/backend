package com.exam.common.util;

import com.exam.auth.dto.UserDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

// ins_ip / upt_ip 감사 컬럼에 넣을 클라이언트 IP, 세션에서 로그인 사용자 꺼내기 등
// 여러 컨트롤러에서 반복되던 자잘한 코드를 모아둔 공용 유틸
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

    // 2026-08-18: 관리자 컨트롤러 6개가 각자 만들어 쓰던 private getLoginUser()를 여기 하나로 통합.
    // 인가(관리자/매니저 여부) 체크 자체는 AdminAccessInterceptor가 컨트롤러 진입 전에 이미
    // 걸러주므로, 컨트롤러 메서드 안에서는 "로그인된 사용자 정보를 꺼내 쓰는 용도"로만 호출하면 됨.
    public static UserDTO getLoginUser(HttpSession session) {
        return (UserDTO) session.getAttribute("loginUser");
    }
}
