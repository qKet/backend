package com.exam.admin.dto;

import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.util.ArrayList;
import java.util.List;

// 로그인 사용자가 접근 가능한 메뉴를 트리 구조로 내려줄 때 쓰는 응답 전용 DTO (GET /common/menus/my)
@Data
@Alias("MenuTreeDTO")
public class MenuTreeDTO {
    private Long menuId;
    private String menuNm;
    private String urlPath;
    private Integer sortOrder;
    private Long parentMenuId;
    private List<MenuTreeDTO> children = new ArrayList<>();
}
