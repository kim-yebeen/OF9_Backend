package com.nine.baseballdiary.backend.record;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateRecordRequest {
    @NotNull
    private Long userId;             // 인증된 사용자 ID

    @NotBlank
    private String gameId;              // KBO gamePK

    private String seatInfo;            // optional
    @NotNull
    private String ticketImageUrl;      // optional

    @NotNull
    private Integer emotionCode;        // 이모지 번호(필수)

    private String comment;             // 한줄평
    private String bestPlayer;          // 베스트 플레이어
    private List<String> foodTags;      // 음식 태그 리스트
    private List<String> mediaUrls;     // 사진/동영상 URL 리스트
}