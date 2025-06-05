package com.nine.baseballdiary.backend.reaction;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ReactionRequest {
    private Long userId;
    private Integer reactionTypeId;
}


