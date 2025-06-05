package com.nine.baseballdiary.backend.reaction;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class RecordReactionSummary {
    private List<ReactionStatsResponse> stats;
    private Integer totalCount;

}
