package com.nine.baseballdiary.backend.reaction;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReactionResponse {
    private String category;
    private String name;
    private String emoji;
    private Integer count;
    private Boolean isMyReaction; // 내가 누른 리액션인지 여부
}