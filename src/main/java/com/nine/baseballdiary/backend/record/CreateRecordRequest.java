package com.nine.baseballdiary.backend.record;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateRecordRequest {
    private long userId;
    // 필수 정보
    private String gameId;
    private String stadium;
    private String seatInfo;
    private Integer emotionCode;

    // 선택 정보 (한번에 다 받을 수 있음)
    private String comment;
    private String longContent;
    private String bestPlayer;
    private List<String> companions;
    private List<String> foodTags;
    private List<String> mediaUrls;
}