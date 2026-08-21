package com.exam.review.mapper;

import com.exam.review.dto.ReviewDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReviewMapper {
    int save(ReviewDTO reviewDTO);
    List<ReviewDTO> findByPerformanceId(Long performanceId);
    ReviewDTO findById(Long reviewId);
    // use_yn 상관없이 조회 — 회차당 1개 UNIQUE 제약이 use_yn과 무관하게 걸려있어서,
    // 삭제(use_yn='N')된 행이 있는지 먼저 확인해 재작성 시 새로 INSERT하지 않고 그 행을 되살리기 위함
    ReviewDTO findByRoundAndUser(@Param("roundId") Long roundId, @Param("userId") String userId);
    int update(ReviewDTO reviewDTO);
    int markDeleted(@Param("reviewId") Long reviewId, @Param("uptId") String uptId, @Param("uptIp") String uptIp);
}
