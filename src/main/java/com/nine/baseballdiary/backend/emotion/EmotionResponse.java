package com.nine.baseballdiary.backend.emotion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmotionResponse {
    private Integer code;
    private String label;
    private Integer displayOrder;
}