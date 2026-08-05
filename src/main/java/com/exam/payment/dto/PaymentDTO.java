package com.exam.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.time.LocalDateTime;

@Data
@Alias("PaymentDTO")
public class PaymentDTO {

    private Long paymentId;
    private Long reservationId;
    private String userId;
    private String orderId;
    private String paymentKey;
    private Long amount;
    private String payStatus;
    private LocalDateTime approvedAt;

    // JOIN 결과용 필드 (마이페이지 결제내역 표시용)
    private String seatRow;
    private String seatColume;
    private String grade;
    @JsonProperty("pTitle")
    private String pTitle;
    private LocalDateTime roundTime;

    private String insId;
    private String insIp;
    private String uptId;
    private String uptIp;
}
