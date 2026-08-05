package com.exam.reservation.dto;

import lombok.Data;
import org.apache.ibatis.type.Alias;

@Data
@Alias("RoundDTO")
public class RoundDTO {

    private Long roundId;
    private Long performanceId;
    private String roundTime;
    private String openTime;
    private String roundStatus;

    // 감사(audit) 컬럼 — 등록자/수정자 ID·IP
    private String insId;
    private String insIp;
    private String uptId;
    private String uptIp;
}
