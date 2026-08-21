package com.exam.review.dto;

import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.time.LocalDateTime;

@Data
@Alias("ReviewDTO")
public class ReviewDTO {

    private Long reviewId;
    private Long performanceId;
    private Long roundId;
    private String userId;
    private String content;
    private Integer rating;
    private String containsSpoiler;
    private String useYn;
    private LocalDateTime insDe;

    // JOIN 결과용 필드 (목록 표시용 작성자 이름/회차 일시)
    private String userNm;
    private LocalDateTime roundTime;

    private String insId;
    private String insIp;
    private String uptId;
    private String uptIp;
}
