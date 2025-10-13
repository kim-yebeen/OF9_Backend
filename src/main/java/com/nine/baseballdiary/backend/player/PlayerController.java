package com.nine.baseballdiary.backend.player;

import com.nine.baseballdiary.backend.common.response.ApiResponse;
import com.nine.baseballdiary.backend.report.dto.PlayerInfoDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/players")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerService playerService;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<PlayerInfoDto>>> searchPlayers(
            @RequestParam String q) {
        List<PlayerInfoDto> players = playerService.searchPlayers(q);
        return ResponseEntity.ok(ApiResponse.success("선수를 검색했습니다", players));
    }
}