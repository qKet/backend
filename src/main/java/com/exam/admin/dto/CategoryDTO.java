package com.exam.admin.dto;

import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.time.LocalDateTime;

@Data
@Alias("CategoryDTO")
public class CategoryDTO {
    private Long categoryId;
    private String categoryNm;
    private Integer sortOrder;
    private String useYn;

    // 감사(audit) 컬럼 — 등록자/수정자 ID·IP·일시
    private String insId;
    private String insIp;
    private LocalDateTime insDe;
    private String uptId;
    private String uptIp;
    private LocalDateTime uptDe;
}
