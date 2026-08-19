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

// 2026-08-18: 관리자용 API(/admin/**, /manage/**)를 요청이 컨트롤러에 도달하기 전에 한 곳에서
// 걸러주는 문지기. 예전엔 각 컨트롤러(AdminController, AdminCategoryController,
// AdminReservationController, ProgramController, MenuController, AdminPerformanceController)가
// 메서드마다 "if (!isAdmin(...)) throw ..." 를 직접 복붙해서 넣고 있었음 — 새 관리자 API를
// 추가할 때 이 체크를 깜빡하면 그대로 인가 우회가 되는 구조라, preHandle()에서 경로 기준으로
// 자동으로 막아서 이 실수 자체가 날 수 없게 함.
//
// 여기서 던지는 BusinessException은 컨트롤러에서 던질 때와 마찬가지로 GlobalExceptionHandler가
// 받아서 처리함(인터셉터도 DispatcherServlet의 같은 예외 처리 체인을 타므로) — 응답 형식은
// 기존과 동일하게 유지됨.
@Component
public class AdminAccessInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        // getSession(false): 세션이 없으면 새로 만들지 않고 null 반환 (비로그인 요청 때문에
        // 불필요한 빈 세션이 계속 생기는 걸 방지). 세션 자체가 없으면 당연히 로그인된 사용자도 없음.
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
