package com.exam.review.service;

import com.exam.reservation.dto.ReservationDTO;
import com.exam.review.dto.ReviewDTO;

import java.util.List;

public interface ReviewService {
    ReviewDTO write(Long performanceId, Long roundId, String userId, String content, int rating, boolean containsSpoiler, String clientIp);
    List<ReviewDTO> list(Long performanceId);
    List<ReservationDTO> reviewableRounds(Long performanceId, String userId);
    ReviewDTO update(Long reviewId, String userId, String content, int rating, boolean containsSpoiler, String clientIp);
    void delete(Long reviewId, String userId, String clientIp);
}
