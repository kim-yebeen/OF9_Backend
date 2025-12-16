package com.nine.baseballdiary.backend.complaints;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CreateComplaintRequest {
    private Long reportedUserId;     // 사용자 신고 (선택)
    private Long reportedRecordId;   // 게시글 신고 (선택)
}