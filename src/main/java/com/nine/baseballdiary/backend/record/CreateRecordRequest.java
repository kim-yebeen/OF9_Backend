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
    private Long userId;
    @NotBlank
    private String gameId;
    @NotBlank
    private String stadium;
    @NotBlank
    private String seatInfo;
    @NotNull
    private Integer emotionCode;
}