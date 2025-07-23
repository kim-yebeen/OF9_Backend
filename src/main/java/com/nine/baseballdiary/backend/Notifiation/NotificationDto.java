package com.nine.baseballdiary.backend.Notifiation;

import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class NotificationDto {
    private Long id;
    private String type;
    private String content;
    private Boolean isRead;
    private String timeAgo;
    private String createdAt;
    private String userNickname;
    private String userProfileImage;
    private Long relatedRecordId;
    private String emotionName;
    private Integer emotionCode;
}