package com.nine.baseballdiary.backend.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BlockedUserDto {
    private Long userId;
    private String nickname;
    private String profileImageUrl;
    private String favTeam;
    private LocalDateTime blockedAt;
}