package com.nine.baseballdiary.backend.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRecordResponse {
    private List<SearchRecordDto> records;
    private int currentPage;
    private int totalPages;
    private long totalElements;
    private boolean hasNext;
}
