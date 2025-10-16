package com.nine.baseballdiary.backend.report.controller;

import com.nine.baseballdiary.backend.common.response.ApiResponse;
import com.nine.baseballdiary.backend.report.dto.*;
import com.nine.baseballdiary.backend.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.parseLong((String) auth.getPrincipal());
    }

    //리포트 메인 통합
    @GetMapping("/main")
    public ResponseEntity<ApiResponse<MainReportResponseDto>> getMainReport() {
        MainReportResponseDto report = reportService.getMainReport(getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("메인 리포트를 조회했습니다.", report));
    }

    //뱃지 상세보기
    @GetMapping("/badges")
    public ResponseEntity<ApiResponse<BadgeResponseDto>> getBadgeStatus() {
        BadgeResponseDto badges = reportService.getBadgeStatus(getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("뱃지 현황을 조회했습니다.", badges));
    }

}