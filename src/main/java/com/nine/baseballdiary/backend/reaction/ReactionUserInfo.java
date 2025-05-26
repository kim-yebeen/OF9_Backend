package com.nine.baseballdiary.backend.reaction;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReactionUserInfo {
    private Long userId;
    private String nickname;
    private String profileImageUrl;
    private String favTeam;
    private String reactionName;
    private String reactionEmoji;
}