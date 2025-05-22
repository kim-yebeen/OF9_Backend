package com.nine.baseballdiary.backend.user.dto;

import com.nine.baseballdiary.backend.user.entity.FollowRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class FollowRequestDto {
    private Long   requestId;
    private Long   requesterId;
    private String requesterNickname;
    private LocalDateTime requestedAt;
}
