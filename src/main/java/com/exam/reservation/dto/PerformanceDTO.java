package com.exam.reservation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Alias("PerformanceDTO")
public class PerformanceDTO {

    private Long performanceId;
    private Long venueId;
    private Long categoryId;
    private String categoryNm;

    //소문자 한글자로 인해서 camel-case 가 안먹음
    @JsonProperty("pTitle")
    private String pTitle;
    @JsonProperty("pLocation")
    private String pLocation;
    private String posterUrl;
    private LocalDateTime createdPer;
    private List<RoundDTO> rounds;

    // casting 필드
    private List<CastDTO> casts;

    // 감사(audit) 컬럼 — 등록자/수정자 ID·IP
    private String insId;
    private String insIp;
    private String uptId;
    private String uptIp;
}
