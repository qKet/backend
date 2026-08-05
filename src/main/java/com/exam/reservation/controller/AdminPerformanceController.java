package com.exam.reservation.controller;

import com.exam.auth.dto.UserDTO;
import com.exam.common.exception.BusinessException;
import com.exam.common.exception.ErrorCode;
import com.exam.common.util.WebUtil;
import com.exam.reservation.dto.PerformanceDTO;
import com.exam.reservation.dto.RoundDTO;
import com.exam.reservation.dto.VenueDTO;
import com.exam.reservation.mapper.PerformanceMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// 공연/회차/공연장 관리 — 사용자·권한 관리(AdminController)와는 별개 도메인이라 분리함
// (같은 데이터를 다루는 PerformanceMapper, PerformanceController(공개 조회용)와 같은 패키지에 둠)
// 포스터 업로드는 CommonController(/common/upload)로 옮겨감 — 여러 도메인에서 재사용할 수 있게
@Slf4j
@RestController
@RequestMapping("/manage")
public class AdminPerformanceController {

    private final PerformanceMapper performanceMapper;

    public AdminPerformanceController(PerformanceMapper performanceMapper) {
        this.performanceMapper = performanceMapper;
    }

    private UserDTO getLoginUser(HttpSession session) {
        return (UserDTO) session.getAttribute("loginUser");
    }

    private boolean isManagerOrAdmin(UserDTO user) {
        return user != null && (Long.valueOf(2L).equals(user.getRoleId()) || Long.valueOf(3L).equals(user.getRoleId()));
    }

    /***********************************
     *  URL      :   "/venues"
     *  이름      :   공연장 목록 조회
     *  기능      :   공연장 목록 조회
     *  method   :   Get
     ************************************/
    // 공연장 목록 — 매니저(2) 이상
    @GetMapping("/venues")
    public List<VenueDTO> getVenues(HttpSession session) {
        if (!isManagerOrAdmin(getLoginUser(session)))
            throw new BusinessException(ErrorCode.FORBIDDEN);
        return performanceMapper.findAllVenues();
    }

    /***********************************
     *  URL      :   "/events"
     *  이름      :   공연 등록
     *  기능      :   새로운 공연과 회차를 등록
     *  method   :   Post
     ************************************/
    // 공연 추가 (회차 포함) — 매니저(2) 이상
    @Transactional
    @PostMapping("/events")
    public Map<String, Object> createPerformance(@RequestBody PerformanceDTO dto, HttpSession session,
                                               HttpServletRequest request) {
        UserDTO loginUser = getLoginUser(session);
        if (!isManagerOrAdmin(loginUser))
            throw new BusinessException(ErrorCode.FORBIDDEN);
        String actorId = loginUser.getUserId();
        String clientIp = WebUtil.getClientIp(request);
        dto.setInsId(actorId);
        dto.setInsIp(clientIp);
        performanceMapper.insert(dto);
        if (dto.getRounds() != null) {
            for (RoundDTO round : dto.getRounds()) {
                round.setPerformanceId(dto.getPerformanceId());
                round.setInsId(actorId);
                round.setInsIp(clientIp);
                performanceMapper.insertRound(round);
                performanceMapper.initReservationSlots(round.getRoundId(), dto.getPerformanceId(), actorId, clientIp);
            }
        }
        return Map.of("success", true, "performanceId", dto.getPerformanceId());
    }

    /***********************************
     *  URL      :   "/events/{performanceId}"
     *  이름      :   공연 및 회차 수정
     *  기능      :   공연 수정 (제목, 포스터, 회차 포함)
     *  method   :   Put
     ************************************/
    // 공연 수정 (제목, 포스터, 회차 포함) — 매니저(2) 이상
    @Transactional
    @PutMapping("/events/{performanceId}")
    public Map<String, Object> updatePerformance(@PathVariable Long performanceId,
                                               @RequestBody PerformanceDTO dto,
                                               HttpSession session,
                                               HttpServletRequest request) {
        UserDTO loginUser = getLoginUser(session);
        if (!isManagerOrAdmin(loginUser))
            throw new BusinessException(ErrorCode.FORBIDDEN);
        String actorId = loginUser.getUserId();
        String clientIp = WebUtil.getClientIp(request);
        dto.setPerformanceId(performanceId);
        dto.setUptId(actorId);
        dto.setUptIp(clientIp);
        performanceMapper.updatePerformance(dto);
        if (dto.getRounds() != null) {
            for (RoundDTO round : dto.getRounds()) {
                if (!performanceMapper.hasPassedRoundById(round.getRoundId())) {
                    round.setUptId(actorId);
                    round.setUptIp(clientIp);
                    performanceMapper.updateRound(round);
                }
            }
        }
        return Map.of("success", true);
    }

    /***********************************
     *  URL      :   "/events/{performanceId}"
     *  이름      :   공연 삭제
     *  기능      :   공연 삭제 — 오픈된 회차 있으면 거부
     *  method   :   Delete
     ************************************/
    // 공연 삭제 — 오픈된 회차 있으면 거부 — 매니저(2) 이상
    @Transactional
    @DeleteMapping("/events/{performanceId}")
    public Map<String, Object> deletePerformance(@PathVariable Long performanceId, HttpSession session) {
        if (!isManagerOrAdmin(getLoginUser(session)))
            throw new BusinessException(ErrorCode.FORBIDDEN);
        if (performanceMapper.hasPassedRound(performanceId))
            throw new BusinessException(ErrorCode.ROUND_ALREADY_OPEN, "예매 오픈된 회차가 있어 삭제할 수 없습니다.");
        performanceMapper.deleteReservationHistoryByPerformanceId(performanceId);
        performanceMapper.deleteReservationsByPerformanceId(performanceId);
        performanceMapper.deleteRoundsByPerformanceId(performanceId);
        performanceMapper.deletePerformance(performanceId);
        return Map.of("success", true);
    }

    /***********************************
     *  URL      :   "/events/{performanceId}/rounds/{roundId}"
     *  이름      :   회차 수정
     *  기능      :   회차 수정 — 오픈 시간 지나면 거부
     *  method   :   Put
     ************************************/
    // 회차 수정 — 오픈 시간 지나면 거부 — 매니저(2) 이상
    @PutMapping("/events/{performanceId}/rounds/{roundId}")
    public Map<String, Object> updateRound(@PathVariable Long performanceId,
                                         @PathVariable Long roundId,
                                         @RequestBody RoundDTO dto,
                                         HttpSession session,
                                         HttpServletRequest request) {
        UserDTO loginUser = getLoginUser(session);
        if (!isManagerOrAdmin(loginUser))
            throw new BusinessException(ErrorCode.FORBIDDEN);
        if (performanceMapper.hasPassedRoundById(roundId))
            throw new BusinessException(ErrorCode.ROUND_ALREADY_OPEN, "예매 오픈된 회차는 수정할 수 없습니다.");
        dto.setRoundId(roundId);
        dto.setUptId(loginUser.getUserId());
        dto.setUptIp(WebUtil.getClientIp(request));
        performanceMapper.updateRound(dto);
        return Map.of("success", true);
    }

    /***********************************
     *  URL      :   "/events/{performanceId}/rounds/{roundId}"
     *  이름      :   공연 회차 삭제
     *  기능      :   회차 삭제 — 오픈 시간 지나면 거부
     *  method   :   Delete
     ************************************/
    // 회차 삭제 — 오픈 시간 지나면 거부 — 매니저(2) 이상
    @Transactional
    @DeleteMapping("/events/{performanceId}/rounds/{roundId}")
    public Map<String, Object> deleteRound(@PathVariable Long performanceId,
                                         @PathVariable Long roundId,
                                         HttpSession session) {
        if (!isManagerOrAdmin(getLoginUser(session)))
            throw new BusinessException(ErrorCode.FORBIDDEN);
        if (performanceMapper.hasPassedRoundById(roundId))
            throw new BusinessException(ErrorCode.ROUND_ALREADY_OPEN, "예매 오픈된 회차는 삭제할 수 없습니다.");
        performanceMapper.deleteReservationHistoryByRoundId(roundId);
        performanceMapper.deleteReservationsByRoundId(roundId);
        performanceMapper.deleteRound(roundId);
        return Map.of("success", true);
    }

    /***********************************
     *  URL      :   "/events/{performanceId}/rounds"
     *  이름      :   공연 회차 추가
     *  기능      :   회차 추가 + 예약 슬롯 초기화
     *  method   :   Post
     ************************************/
    // 회차 추가 + 예약 슬롯 초기화 — 매니저(2) 이상
    @Transactional
    @PostMapping("/events/{performanceId}/rounds")
    public Map<String, Object> addRound(@PathVariable Long performanceId,
                                      @RequestBody RoundDTO dto,
                                      HttpSession session,
                                      HttpServletRequest request) {
        UserDTO loginUser = getLoginUser(session);
        if (!isManagerOrAdmin(loginUser))
            throw new BusinessException(ErrorCode.FORBIDDEN);
        String actorId = loginUser.getUserId();
        String clientIp = WebUtil.getClientIp(request);
        dto.setPerformanceId(performanceId);
        dto.setInsId(actorId);
        dto.setInsIp(clientIp);
        performanceMapper.insertRound(dto);
        performanceMapper.initReservationSlots(dto.getRoundId(), performanceId, actorId, clientIp);
        return Map.of("success", true, "roundId", dto.getRoundId());
    }
}
