package com.exam.admin.dto;

import lombok.Data;
import org.apache.ibatis.type.Alias;

@Data
@Alias("CategoryDTO")
public class CategoryDTO {
    private Long categoryId;
    private String categoryNm;
    private Integer sortOrder;
    private String useYn;

    // 감사(audit) 컬럼 — 등록자/수정자 ID·IP
    private String insId;
    private String insIp;
    private String uptId;
    private String uptIp;
}
