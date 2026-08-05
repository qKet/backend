package com.exam.reservation.mapper;

import com.exam.reservation.dto.ReservationDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReservationMapper {
    int save(ReservationDTO reservationDTO);
    int insertHistory(ReservationDTO reservationDTO);
    List<ReservationDTO> findByUserId(String userId);
    ReservationDTO findById(Long reservationId);
    int cancel(@Param("reservationId") Long reservationId, @Param("uptId") String uptId, @Param("uptIp") String uptIp);
}
