package com.exam.notification.mapper;

import com.exam.notification.dto.CancelAlertDTO;
import com.exam.notification.dto.CancelAlertRecipientDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CancelAlertMapper {

    String findUseYn(@Param("userId") String userId, @Param("roundId") Long roundId);

    void upsert(CancelAlertDTO dto);

    List<CancelAlertRecipientDTO> findActiveSubscribersByRoundId(@Param("roundId") Long roundId);

    void deleteByRoundId(@Param("roundId") Long roundId);

    void deleteByPerformanceId(@Param("performanceId") Long performanceId);
}
