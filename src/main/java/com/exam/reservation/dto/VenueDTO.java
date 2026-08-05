package com.exam.reservation.dto;

import lombok.Data;
import org.apache.ibatis.type.Alias;

@Data
@Alias("VenueDTO")
public class VenueDTO {
    private Long venueId;
    private String venueName;
}
