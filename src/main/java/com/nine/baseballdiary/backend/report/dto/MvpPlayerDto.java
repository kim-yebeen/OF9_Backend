package com.nine.baseballdiary.backend.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MvpPlayerDto {
    private String playerName;
    private String team;
    private String teamCode;
    private int count;
    private String playerImageUrl;
}