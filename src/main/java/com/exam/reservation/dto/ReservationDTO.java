package com.exam.reservation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.time.LocalDateTime;

@Data
@Alias("ReservationDTO")
public class ReservationDTO {

    private Long reservationId;
    private Long historyId;
    private String userId;
    private Long seatId;
    private Long roundId;
    private String reservedStatus;
    private LocalDateTime createdReserved;

    // 히스토리 insert 용
    private String action;

    // JOIN 결과용 필드
    private String seatRow;
    private String seatColume;
    private String grade;
    @JsonProperty("pTitle")
    private String pTitle;
    private LocalDateTime roundTime;

    // 감사(audit) 컬럼 — 등록자/수정자 ID·IP
    private String insId;
    private String insIp;
    private String uptId;
    private String uptIp;
}
