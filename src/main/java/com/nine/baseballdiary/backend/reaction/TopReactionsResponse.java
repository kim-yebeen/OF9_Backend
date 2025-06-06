package com.nine.baseballdiary.backend.reaction;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class TopReactionsResponse {
    private List<ReactionStatsResponse> top3Reactions;  // 상위 3개
    private Integer remainingCount;                     // 나머지 개수 (외 N개)
}