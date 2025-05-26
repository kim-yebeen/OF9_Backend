package com.nine.baseballdiary.backend.reaction;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AddReactionRequest {
    private Long userId;
    private Long reactionTypeId;
}


