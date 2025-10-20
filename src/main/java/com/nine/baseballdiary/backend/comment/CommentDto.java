package com.nine.baseballdiary.backend.comment;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDto {
    private Long id;
    private Long recordId;
    private Long userId;
    private String nickname;
    private String profileImageUrl;
    private String favTeam;
    private String content;
    private String createdAt;
    private String updatedAt;
    private boolean isEdited;  // 수정 여부
    private boolean isAuthor;  // 현재 사용자가 작성자인지
    private Long replyCount;   // 대댓글 개수
    private List<CommentDto> replies;  // 대댓글 목록
    private Long totalCommentCount;
}