package com.exam.reservation.dto;

import lombok.Data;
import org.apache.ibatis.type.Alias;

// 예매확정/취소 알림 메일 본문에 넣을 표시정보(공연명/회차시각/좌석) — ReservationNotificationService가 씀
@Data
@Alias("SeatDisplayInfoDTO")
public class SeatDisplayInfoDTO {
    private String pTitle;
    private String roundTime;
    private String seatRow;
    private String seatColume;
    private String grade;
}
