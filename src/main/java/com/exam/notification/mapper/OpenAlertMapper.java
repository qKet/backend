package com.exam.notification.mapper;

import com.exam.notification.dto.OpenAlertDTO;
import com.exam.notification.dto.OpenAlertRecipientDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OpenAlertMapper {

    String findUseYn(@Param("userId") String userId, @Param("roundId") Long roundId);

    void upsert(OpenAlertDTO dto);

    // 아직 안 보낸(notified_yn='N') 구독 중 open_time이 [now, windowEnd] 사이로 다가온 것들 조회 —
    // NotificationServiceImpl의 @Scheduled 스위퍼가 씀. DB 서버의 NOW()를 안 쓰고 JVM(TZ=Asia/Seoul로 고정됨)이
    // 계산한 시각을 그대로 넘기는 이유: RDS 서버 자체 시간대가 UTC라 NOW()를 쓰면 KST로 저장된 open_time과
    // 9시간 어긋나서 항상 매칭에 실패했었음(2026-08-18 발견)
    List<OpenAlertRecipientDTO> findDueAlerts(@Param("now") LocalDateTime now, @Param("windowEnd") LocalDateTime windowEnd);

    void markNotified(@Param("alertId") Long alertId);

    void deleteByRoundId(@Param("roundId") Long roundId);

    void deleteByPerformanceId(@Param("performanceId") Long performanceId);
}
