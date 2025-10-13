package com.nine.baseballdiary.backend.Notifiation;

public enum NotificationType {
    FOLLOW("팔로우"),              // 공개 계정 팔로우
    FOLLOW_REQUEST("팔로우 요청"),   // 비공개 계정 팔로우 요청
    LIKE("좋아요"),                 // 게시물 좋아요
    COMMENT("댓글"),                // 게시물에 댓글 작성
    REPLY("답글"),                  // 내 댓글에 답글 작성
    NEW_RECORD("새 게시글"),        // 친구가 새 게시글 작성
    SYSTEM("시스템");               // 앱 소식

    private final String description;

    NotificationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}