package com.nine.baseballdiary.backend.report.controller;

import com.nine.baseballdiary.backend.common.response.ApiResponse;
import com.nine.baseballdiary.backend.report.dto.*;
import com.nine.baseballdiary.backend.report.service.PlayerDataService;
import com.nine.baseballdiary.backend.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.parseLong((String) auth.getPrincipal());
    }

    @GetMapping("/emotions")
    public ResponseEntity<ApiResponse<EmotionSummaryDto>> getEmotionSummary(
            @RequestParam int year, @RequestParam int month) {
        EmotionSummaryDto summary = reportService.getEmotionSummary(getCurrentUserId(), year, month);
        return ResponseEntity.ok(ApiResponse.success("월간 감정 분석을 조회했습니다.", summary));
    }

    @GetMapping("/win-rate")
    public ResponseEntity<ApiResponse<WinRateSummaryDto>> getWinRateSummary() {
        WinRateSummaryDto summary = reportService.getWinRateSummary(getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("직관 승률을 조회했습니다.", summary));
    }

    @GetMapping("/my-ranking")
    public ResponseEntity<ApiResponse<MyRankingDto>> getMyRanking() {
        MyRankingDto ranking = reportService.getMyRanking(getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("내 랭킹 정보를 조회했습니다.", ranking));
    }

    @GetMapping("/ranking")
    public ResponseEntity<ApiResponse<List<UserRankingDto>>> getUserRankings() {
        List<UserRankingDto> rankings = reportService.getUserRankings();
        return ResponseEntity.ok(ApiResponse.success("전체 랭킹을 조회했습니다.", rankings));
    }

    @GetMapping("/badges")
    public ResponseEntity<ApiResponse<BadgeResponseDto>> getBadgeStatus() {
        BadgeResponseDto badges = reportService.getBadgeStatus(getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("뱃지 현황을 조회했습니다.", badges));
    }

    @GetMapping("/season-dday")
    public ResponseEntity<ApiResponse<SeasonDdayDto>> getSeasonDday() {
        SeasonDdayDto dday = reportService.getSeasonDday();
        return ResponseEntity.ok(ApiResponse.success("시즌 D-day를 조회했습니다.", dday));
    }

    @GetMapping("/mvp-players")
    public ResponseEntity<ApiResponse<List<MvpPlayerDto>>> getMvpPlayers() {
        Long userId = getCurrentUserId();
        List<MvpPlayerDto> mvps = reportService.getMvpPlayers(userId);
        return ResponseEntity.ok(ApiResponse.success("MVP 선수를 조회했습니다.", mvps));
    }

    @GetMapping("/companions")
    public ResponseEntity<ApiResponse<List<CompanionStatsDto>>> getCompanionStats() {
        Long userId = getCurrentUserId();
        List<CompanionStatsDto> stats = reportService.getCompanionStats(userId);
        return ResponseEntity.ok(ApiResponse.success("직관 메이트를 조회했습니다.", stats));
    }

    @GetMapping("/players")
    public ResponseEntity<ApiResponse<List<PlayerInfoDto>>> getPlayersByTeam(
            @RequestParam(required = false) String team) {
        PlayerDataService playerService = new PlayerDataService();
        List<PlayerInfoDto> players = team != null
                ? playerService.getPlayersByTeam(team)
                : playerService.getAllPlayers();
        return ResponseEntity.ok(ApiResponse.success("선수 목록을 조회했습니다.", players));
    }
}