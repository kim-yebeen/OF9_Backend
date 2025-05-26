package com.nine.baseballdiary.backend.reaction;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class RecordReactionSummary {
    private Long recordId;
    private List<ReactionResponse> reactions;
    private List<ReactionUserInfo> reactionUsers;
    private Integer totalCount;
}