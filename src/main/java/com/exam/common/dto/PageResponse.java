package com.exam.common.dto;

import lombok.Getter;

import java.util.List;

// 목록 API에 페이지네이션을 적용할 때 공통으로 쓰는 응답 DTO.
// GlobalResponseAdvice가 이 객체도 그대로 { success, message, data, timestamp }로 감싸주므로
// 컨트롤러는 data 자리에 이 타입을 그대로 리턴하면 됨.
@Getter
public class PageResponse<T> {

    private final List<T> content;
    private final int page;
    private final int size;
    private final long totalCount;
    private final int totalPages;

    public PageResponse(List<T> content, int page, int size, long totalCount) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalCount = totalCount;
        this.totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalCount / size);
    }
}
