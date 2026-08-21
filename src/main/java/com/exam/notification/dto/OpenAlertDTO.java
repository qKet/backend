package com.exam.notification.dto;

import lombok.Data;
import org.apache.ibatis.type.Alias;

// OPEN_ALERTS 행 자체 — 구독 토글(subscribe/unsubscribe)용
@Data
@Alias("OpenAlertDTO")
public class OpenAlertDTO {

    private Long alertId;
    private String userId;
    private Long roundId;
    private String useYn;

    // 감사(audit) 컬럼 — 구독/해지 행위자 ID·IP (본인 행위라 userId와 항상 동일)
    private String insId;
    private String insIp;
}
