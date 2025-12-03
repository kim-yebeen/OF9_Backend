package com.nine.baseballdiary.backend.stadium;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class StadiumSeatResponse {
    private String stadiumName;
    private List<String> zones; // 구역 이름 목록
}