package com.nine.baseballdiary.backend.record;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.util.List;

@Data
@NoArgsConstructor
public class CreateDetailRequest {
    @NotNull(message = "감정 이모지는 반드시 선택해야 합니다.")
    private String emotionEmoji;

    private String comment;
    private String bestPlayer;
    private List<String> foodTags;
    private List<String> mediaUrls;
    private String result;  // "WIN", "LOSE", "DRAW"
}