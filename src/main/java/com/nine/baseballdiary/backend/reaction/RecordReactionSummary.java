package com.nine.baseballdiary.backend.reaction;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
public class RecordReactionSummary {
    private List<ReactionStatsResponse> stats;
    private Integer totalCount;
    private String myReaction;

    // myReaction이 없는 경우를 위한 생성자
    public RecordReactionSummary(List<ReactionStatsResponse> stats, Integer totalCount) {
        this.stats = stats;
        this.totalCount = totalCount;
        this.myReaction = null;
    }

    // myReaction이 있는 경우를 위한 생성자
    public RecordReactionSummary(List<ReactionStatsResponse> stats, Integer totalCount, String myReaction) {
        this.stats = stats;
        this.totalCount = totalCount;
        this.myReaction = myReaction;
    }
}