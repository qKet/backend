package com.exam.review.service;

import com.exam.common.exception.BusinessException;
import com.exam.common.exception.ErrorCode;
import com.exam.reservation.dto.ReservationDTO;
import com.exam.reservation.service.ReservationService;
import com.exam.review.dto.ReviewDTO;
import com.exam.review.mapper.ReviewMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewMapper reviewMapper;
    private final ReservationService reservationService;
    private final SpoilerDetectionService spoilerDetectionService;

    public ReviewServiceImpl(ReviewMapper reviewMapper, ReservationService reservationService,
                              SpoilerDetectionService spoilerDetectionService) {
        this.reviewMapper = reviewMapper;
        this.reservationService = reservationService;
        this.spoilerDetectionService = spoilerDetectionService;
    }

    /***********************************
     *  이름      :  write
     *  기능      :  감상평 작성 — 그 회차 예매자만 가능, 회차당 1개만 허용. 스포일러 여부는
     *              사용자가 직접 체크하지 않고 AI(AI01_SPOIL01)가 본문을 보고 자동 판별한다
     *  param    :  Long, Long, String, String, int, String
     *  return   :  ReviewDTO
     ************************************/
    @Override
    @Transactional
    public ReviewDTO write(Long performanceId, Long roundId, String userId, String content, int rating, String clientIp) {
        requireValidRating(rating);
        if (!reservationService.hasReservation(userId, roundId)) {
            throw new BusinessException(ErrorCode.REVIEW_WRITE_NOT_ALLOWED);
        }

        String containsSpoiler = spoilerDetectionService.isSpoiler(content) ? "Y" : "N";

        // round_id+user_id UNIQUE 제약이 use_yn과 무관하게 걸려있어서, 예전에 삭제(use_yn='N')했던
        // 회차 리뷰가 있으면 새로 INSERT하지 않고 그 행을 되살림 (안 그러면 제약 위반으로 에러남)
        ReviewDTO existing = reviewMapper.findByRoundAndUser(roundId, userId);
        if (existing != null) {
            if ("Y".equals(existing.getUseYn())) {
                throw new BusinessException(ErrorCode.REVIEW_ALREADY_EXISTS);
            }
            existing.setContent(content);
            existing.setRating(rating);
            existing.setContainsSpoiler(containsSpoiler);
            existing.setUptId(userId);
            existing.setUptIp(clientIp);
            reviewMapper.update(existing);
            return reviewMapper.findById(existing.getReviewId());
        }

        ReviewDTO review = new ReviewDTO();
        review.setPerformanceId(performanceId);
        review.setRoundId(roundId);
        review.setUserId(userId);
        review.setContent(content);
        review.setRating(rating);
        review.setContainsSpoiler(containsSpoiler);
        review.setInsId(userId);
        review.setInsIp(clientIp);

        reviewMapper.save(review);
        return reviewMapper.findById(review.getReviewId());
    }

    /***********************************
     *  이름      :  list
     *  기능      :  공연별 감상평 목록 조회 (공개, use_yn='Y'만)
     *  param    :  Long
     *  return   :  List<ReviewDTO>
     ************************************/
    @Override
    public List<ReviewDTO> list(Long performanceId) {
        return reviewMapper.findByPerformanceId(performanceId);
    }

    /***********************************
     *  이름      :  reviewableRounds
     *  기능      :  이 공연에서 사용자가 예매한 회차 목록 (감상평 작성 화면의 회차 선택용)
     *  param    :  Long, String
     *  return   :  List<ReservationDTO>
     ************************************/
    @Override
    public List<ReservationDTO> reviewableRounds(Long performanceId, String userId) {
        return reservationService.getReservedRounds(userId, performanceId);
    }

    /***********************************
     *  이름      :  update
     *  기능      :  감상평 수정 (본인만). 스포일러 여부는 수정된 본문 기준으로 AI가 다시 판별한다
     *  param    :  Long, String, String, int, String
     *  return   :  ReviewDTO
     ************************************/
    @Override
    @Transactional
    public ReviewDTO update(Long reviewId, String userId, String content, int rating, String clientIp) {
        requireValidRating(rating);
        ReviewDTO review = requireOwnedReview(reviewId, userId);

        review.setContent(content);
        review.setRating(rating);
        review.setContainsSpoiler(spoilerDetectionService.isSpoiler(content) ? "Y" : "N");
        review.setUptId(userId);
        review.setUptIp(clientIp);
        reviewMapper.update(review);

        return reviewMapper.findById(reviewId);
    }

    /***********************************
     *  이름      :  delete
     *  기능      :  감상평 삭제 (본인만, 물리삭제 아니고 use_yn='N' 소프트 삭제)
     *  param    :  Long, String, String
     *  return   :  void
     ************************************/
    @Override
    public void delete(Long reviewId, String userId, String clientIp) {
        requireOwnedReview(reviewId, userId);
        reviewMapper.markDeleted(reviewId, userId, clientIp);
    }

    private void requireValidRating(int rating) {
        if (rating < 1 || rating > 5) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "별점은 1~5 사이여야 합니다.");
        }
    }

    // 본인 소유가 아니면 남의 리뷰가 존재한다는 사실 자체를 숨기기 위해 NOT_FOUND로 위장 (Payment 패턴과 동일)
    private ReviewDTO requireOwnedReview(Long reviewId, String userId) {
        ReviewDTO review = reviewMapper.findById(reviewId);
        if (review == null || !review.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.REVIEW_NOT_FOUND);
        }
        return review;
    }
}
