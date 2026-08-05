package com.exam.admin.dto;

import lombok.Data;
import org.apache.ibatis.type.Alias;

@Data
@Alias("MenuDTO")
public class MenuDTO {
    private Long menuId;
    private Long programId;
    private String programNm; // PROGRAMS 조인 표시용 (그리드)
    private String urlPath;   // PROGRAMS 조인 표시용 (그리드)
    private Long parentMenuId;
    private String menuNm;
    private Integer sortOrder;
    private String useYn;

    // 감사(audit) 컬럼 — 등록자/수정자 ID·IP
    private String insId;
    private String insIp;
    private String uptId;
    private String uptIp;
}
