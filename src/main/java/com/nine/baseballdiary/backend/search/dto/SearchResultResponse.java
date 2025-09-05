package com.nine.baseballdiary.backend.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultResponse {
    private SearchRecordResponse records;
    private SearchUserResponse users;
}