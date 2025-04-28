package com.nine.baseballdiary.backend.record;

import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
public class CreateDraftRequest {

    @NotBlank(message = "gameId는 필수입니다.")
    private String gameId;

    @NotBlank(message = "gameDate는 필수입니다. (yyyy-MM-dd)")
    private String gameDate;

    @NotBlank(message = "homeTeam은 필수입니다.")
    private String homeTeam;

    @NotBlank(message = "awayTeam은 필수입니다.")
    private String awayTeam;

    @NotBlank(message = "startTime은 필수입니다. (HH:mm)")
    private String startTime;

    private String seatInfo;
}
