package com.exam.admin.controller;

import com.exam.auth.dto.UserDTO;
import com.exam.admin.dto.ProgramDTO;
import com.exam.admin.dto.RoleProgramDTO;
import com.exam.common.exception.BusinessException;
import com.exam.common.exception.ErrorCode;
import com.exam.admin.service.ProgramService;
import com.exam.common.util.WebUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// 프로그램관리(화면 등록) + 권한 확장(역할별 프로그램 접근권한 그리드). 관리자(3)만
@RestController
@RequestMapping("/admin/programs")
public class ProgramController {

    private final ProgramService programService;

    public ProgramController(ProgramService programService) {
        this.programService = programService;
    }

    private UserDTO getLoginUser(HttpSession session) {
        return (UserDTO) session.getAttribute("loginUser");
    }

    private boolean isAdmin(UserDTO user) {
        return user != null && Long.valueOf(3L).equals(user.getRoleId());
    }

    /***********************************
     * URL : "/admin/programs"
     * 이름 : 프로그램 목록 조회
     * 기능 : 등록된 화면(URL) 목록 조회
     * method : Get
     ************************************/
    @GetMapping
    public List<ProgramDTO> getPrograms(HttpSession session) {
        if (!isAdmin(getLoginUser(session)))
            throw new BusinessException(ErrorCode.ADMIN_ONLY);
        return programService.getPrograms();
    }

    /***********************************
     * URL : "/admin/programs"
     * 이름 : 프로그램 등록
     * 기능 : 새 화면(URL) 등록
     * method : Post
     ************************************/
    @PostMapping
    public Map<String, Object> createProgram(@RequestBody ProgramDTO body, HttpSession session,
            HttpServletRequest request) {
        UserDTO loginUser = getLoginUser(session);
        if (!isAdmin(loginUser))
            throw new BusinessException(ErrorCode.ADMIN_ONLY);
        body.setInsId(loginUser.getUserId());
        body.setInsIp(WebUtil.getClientIp(request));
        programService.createProgram(body);
        return Map.of("success", true);
    }

    /***********************************
     * URL : "/admin/programs/{programId}"
     * 이름 : 프로그램 수정
     * 기능 : 화면 정보(이름/경로/타입/사용여부) 수정
     * method : Put
     ************************************/
    @PutMapping("/{programId}")
    public Map<String, Object> updateProgram(@PathVariable Long programId, @RequestBody ProgramDTO body,
            HttpSession session, HttpServletRequest request) {
        UserDTO loginUser = getLoginUser(session);
        if (!isAdmin(loginUser))
            throw new BusinessException(ErrorCode.ADMIN_ONLY);
        body.setProgramId(programId);
        body.setUptId(loginUser.getUserId());
        body.setUptIp(WebUtil.getClientIp(request));
        programService.updateProgram(body);
        return Map.of("success", true);
    }

    /***********************************
     * URL : "/admin/programs/{programId}"
     * 이름 : 프로그램 삭제
     * 기능 : 화면 삭제 (연결된 권한 매핑도 함께 정리)
     * method : Delete
     ************************************/
    @DeleteMapping("/{programId}")
    public Map<String, Object> deleteProgram(@PathVariable Long programId, HttpSession session) {
        if (!isAdmin(getLoginUser(session)))
            throw new BusinessException(ErrorCode.ADMIN_ONLY);
        programService.deleteProgram(programId);
        return Map.of("success", true);
    }

    /***********************************
     * URL : "/admin/programs/role-mappings"
     * 이름 : 역할별 접근권한 조회
     * 기능 : 역할×프로그램 매핑 전체 조회 (권한 그리드)
     * method : Get
     ************************************/
    @GetMapping("/role-mappings")
    public List<RoleProgramDTO> getRoleMappings(HttpSession session) {
        if (!isAdmin(getLoginUser(session)))
            throw new BusinessException(ErrorCode.ADMIN_ONLY);
        return programService.getRolePrograms();
    }

    /***********************************
     * URL : "/admin/programs/role-mappings"
     * 이름 : 역할별 접근권한 저장
     * 기능 : 권한 그리드에서 체크한 역할×프로그램 매핑 전체 교체 저장
     * method : Put
     ************************************/
    @PutMapping("/role-mappings")
    public Map<String, Object> updateRoleMappings(@RequestBody List<RoleProgramDTO> body, HttpSession session,
            HttpServletRequest request) {
        UserDTO loginUser = getLoginUser(session);
        if (!isAdmin(loginUser))
            throw new BusinessException(ErrorCode.ADMIN_ONLY);
        String clientIp = WebUtil.getClientIp(request);
        for (RoleProgramDTO rp : body) {
            rp.setInsId(loginUser.getUserId());
            rp.setInsIp(clientIp);
        }
        programService.updateRolePrograms(body);
        return Map.of("success", true);
    }
}
