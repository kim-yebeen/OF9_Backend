package com.nine.baseballdiary.backend.Notifiation;

public enum NotificationType {
    FOLLOW("팔로우"),              // 공개 계정 팔로우
    FOLLOW_REQUEST("팔로우 요청"),   // 비공개 계정 팔로우 요청
    REACTION("공감"),              // 직관 기록 공감
    NEW_RECORD("새 게시글"),        // ✅ 친구가 새 게시글 작성
    SYSTEM("시스템");      // 앱 소식
    private final String description;

    // 생성자
    NotificationType(String description) {
        this.description = description;
    }

    // getter
    public String getDescription() {
        return description;
    }
}