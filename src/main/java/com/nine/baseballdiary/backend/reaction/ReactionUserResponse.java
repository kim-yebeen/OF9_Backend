package com.nine.baseballdiary.backend.reaction;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ReactionUserResponse {
    private Long userId;
    private String nickname;
    private String profileImageUrl;
    private String favTeam;
    private String reactionName;

    public ReactionUserResponse(Long userId, String nickname, String profileImageUrl, String favTeam, String reactionName) {
        this.userId = userId;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.favTeam = favTeam;
        this.reactionName = reactionName;
    }
}
