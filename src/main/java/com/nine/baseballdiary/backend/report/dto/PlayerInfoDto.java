package com.nine.baseballdiary.backend.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class PlayerInfoDto {
    private String name;
    private String team;
    private String position;
    private String imageUrl;
}