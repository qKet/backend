package com.exam.reservation.dto;

import lombok.Data;
import org.apache.ibatis.type.Alias;

@Data
@Alias("SeatDTO")
public class SeatDTO {

    private Long reservationId;
    private Long seatId;
    private Long roundId;
    private String seatRow;
    private String seatColume;
    private String grade;
    private String status;
}
