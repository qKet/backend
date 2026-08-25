package com.exam.admin.dto;

import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.time.LocalDateTime;

// 역할 × 프로그램 접근권한 매핑 한 건 (권한 그리드의 체크 한 칸)
@Data
@Alias("RoleProgramDTO")
public class RoleProgramDTO {
    private Long roleId;
    private Long programId;
    private String useYn;

    private String insId;
    private String insIp;

    private String uptId;
    private String uptIp;
    private LocalDateTime uptDe;
}
