package com.exam.common.interceptor;

import com.exam.auth.dto.UserDTO;
import com.exam.common.exception.BusinessException;
import com.exam.common.exception.ErrorCode;
import com.exam.common.util.WebUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

// 관리자용 API(/admin/**, /manage/**)를 컨트롤러 도달 전에 한 곳에서 걸러주는 문지기 —
// 각 컨트롤러마다 인가 체크를 복붙하면 새 API 추가 시 깜빡하고 빠뜨릴 위험이 있어서 경로 기준
// 자동 차단으로 바꿈. 여기서 던지는 BusinessException도 GlobalExceptionHandler가 그대로 처리.
@Component
public class AdminAccessInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // getRequestURI()는 context-path(/api)까지 포함해서 "/admin" 매칭이 안 됨 — 관리자 API가
        // 인증 없이 뚫려있던 치명적 버그의 원인이었음. getServletPath()는 context-path 제외 경로를 줌.
        String path = request.getServletPath();
        // getSession(false): 세션 없으면 새로 안 만들고 null(비로그인 요청마다 빈 세션 생성 방지).
        HttpSession session = request.getSession(false);
        UserDTO loginUser = session != null ? WebUtil.getLoginUser(session) : null;

        // /admin/**  → 관리자(roleId 3)만
        // /manage/** → 매니저(roleId 2) 이상 (AdminPerformanceController가 원래 쓰던 기준과 동일)
        if (path.startsWith("/admin")) {
            if (loginUser == null || !Long.valueOf(3L).equals(loginUser.getRoleId())) {
                throw new BusinessException(ErrorCode.ADMIN_ONLY);
            }
        } else if (path.startsWith("/manage")) {
            boolean managerOrAdmin = loginUser != null
                    && (Long.valueOf(2L).equals(loginUser.getRoleId()) || Long.valueOf(3L).equals(loginUser.getRoleId()));
            if (!managerOrAdmin) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
        }

        return true;
    }
}
