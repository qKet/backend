package com.exam.reservation.dto;

import lombok.Data;
import org.apache.ibatis.type.Alias;

@Data
@Alias("CastDTO")
public class CastDTO {
    private Long castId;

    private Long performanceId;
    // roundId가 Null이면 전체 회차 공통 캐스팅에 해당하는거고, 값이 들어간다면 해당 회차만 캐스팅
    private Long roundId;

    private String actorName;
    // castingNm이 NUll이면 배역 개념이 없는 공연 (콘서트)
    private String castingNm;
    private Integer sortOrder;

    // 로그 컬럼
    private String insId;
    private String insIp;
    private String uptId;
    private String uptIp;
}