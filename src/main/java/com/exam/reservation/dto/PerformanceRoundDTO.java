package com.exam.reservation.dto;

import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.time.LocalDateTime;

@Data
@Alias("PerformanceRoundDTO")
public class PerformanceRoundDTO {

    private Long roundId;
    private Long performanceId;
    private String pTitle;
    private LocalDateTime roundTime;
    private String roundStatus;
}
