package com.exam.notification.mapper;

import com.exam.notification.dto.OpenAlertDTO;
import com.exam.notification.dto.OpenAlertRecipientDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OpenAlertMapper {

    String findUseYn(@Param("userId") String userId, @Param("roundId") Long roundId);

    void upsert(OpenAlertDTO dto);

    // 아직 안 보낸(notified_yn='N') 구독 중 open_time이 minutesBefore분 이내로 다가온 것들 조회 —
    // NotificationServiceImpl의 @Scheduled 스위퍼가 씀
    List<OpenAlertRecipientDTO> findDueAlerts(@Param("minutesBefore") int minutesBefore);

    void markNotified(@Param("alertId") Long alertId);

    void deleteByRoundId(@Param("roundId") Long roundId);

    void deleteByPerformanceId(@Param("performanceId") Long performanceId);
}
