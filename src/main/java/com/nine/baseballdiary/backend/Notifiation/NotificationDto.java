package com.nine.baseballdiary.backend.Notifiation;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationDto {
    private Long id;
    private String type;
    private String content;
    private String timeAgo;
    private String createdAt;

    private String userNickname;
    private String userProfileImage;

    private Long relatedRecordId;
    private String emotionName;
    private Integer emotionCode;

    private String actionButton;
    private String category;

    private Boolean isFollowing;  // 내가 이 사람을 팔로우하고 있는지
    private Boolean isFollower;   // 이 사람이 나를 팔로우하고 있는지 (맞팔 확인용)

}