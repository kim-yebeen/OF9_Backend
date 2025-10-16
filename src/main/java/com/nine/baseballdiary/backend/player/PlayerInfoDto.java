package com.nine.baseballdiary.backend.player;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlayerInfoDto {
    private final String name;
    private final String team;
    private final String position;
    private final String imageUrl;
}
