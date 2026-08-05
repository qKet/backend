package com.exam.common.controller;

import com.exam.auth.dto.UserDTO;
import com.exam.admin.dto.MenuTreeDTO;
import com.exam.common.exception.BusinessException;
import com.exam.common.exception.ErrorCode;
import com.exam.common.service.CommonService;
import com.exam.admin.service.MenuService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

// 여러 도메인이 공통으로 쓰는 API. 파일 업로드, 로그인 사용자의 접근 가능 메뉴 조회 등 특정 도메인 전용이 아닌 기능을 모아둠
@RestController
@RequestMapping("/common")
public class CommonController {

    private final CommonService commonService;
    private final MenuService menuService;

    public CommonController(CommonService commonService, MenuService menuService) {
        this.commonService = commonService;
        this.menuService = menuService;
    }

    /***********************************
     * URL : "/common/upload"
     * 이름 : 파일 업로드
     * 기능 : 포스터/프로필 사진 등 이미지 파일을 S3에 업로드 (특정 도메인 전용 아님)
     * method : Post
     * param : MultipartFile, folder(어느 용도로 쓰는 업로드인지), HttpSession
     ************************************/
    // 로그인한 사용자면 누구나 사용 가능 — 매니저 이상만 되는 "이 URL을 공연에 등록"같은 건
    // 업로드 자체가 아니라 그걸 호출하는 각 도메인(AdminPerformanceController 등)에서 따로 체크함
    // 실패 시 예외 처리는 CommonServiceImpl이 BusinessException(ErrorCode.UPLOAD_FAILED)을 직접 던지므로 여기선 안 잡음
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "uploads") String folder,
            HttpSession session) {
        if (session.getAttribute("loginUser") == null)
            throw new BusinessException(ErrorCode.LOGIN_REQUIRED);
        String url = commonService.upload(file, folder);
        return Map.of("success", true, "url", url);
    }

    /***********************************
     * URL : "/common/menus/my"
     * 이름 : 내 메뉴 조회
     * 기능 : 로그인한 사용자의 role이 접근 가능한 메뉴를 트리 구조로 조회 (네비게이션 렌더링용)
     * method : Get
     ************************************/
    @GetMapping("/menus/my")
    public List<MenuTreeDTO> getMyMenus(HttpSession session) {
        UserDTO loginUser = (UserDTO) session.getAttribute("loginUser");
        if (loginUser == null)
            throw new BusinessException(ErrorCode.LOGIN_REQUIRED);
        return menuService.getMyMenuTree(loginUser.getRoleId());
    }
}
