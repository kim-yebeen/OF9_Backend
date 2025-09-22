package com.nine.baseballdiary.backend.search.controller;

import com.nine.baseballdiary.backend.common.response.ApiResponse;
import com.nine.baseballdiary.backend.search.dto.*;
import com.nine.baseballdiary.backend.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    // JWT 토큰에서 현재 사용자 ID 추출 (GlobalExceptionHandler가 예외 처리)
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("인증된 사용자가 아닙니다.");
        }
        return Long.parseLong((String) authentication.getPrincipal());
    }

    // ✅ 1. 통합 검색 (게시글 + 사용자)
    @GetMapping
    public ResponseEntity<ApiResponse<SearchResultResponse>> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int recordPage,
            @RequestParam(defaultValue = "0") int userPage,
            @RequestParam(defaultValue = "15") int recordSize,
            @RequestParam(defaultValue = "10") int userSize) {

        // 검색어 유효성 검사
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("검색어를 입력해주세요");
        }

        Long userId = getCurrentUserId();
        SearchResultResponse result = searchService.search(userId, query.trim(), recordPage, userPage, recordSize, userSize);

        return ResponseEntity.ok(ApiResponse.success("검색이 완료되었습니다", result));
    }

    // ✅ 2. 최근 검색어 조회
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<String>>> getRecentSearches() {
        Long userId = getCurrentUserId();
        List<String> recentSearches = searchService.getRecentSearches(userId);

        return ResponseEntity.ok(ApiResponse.success("최근 검색어를 조회했습니다", recentSearches));
    }

    // ✅ 3. 최근 검색어 개별 삭제
    @DeleteMapping("/recent")
    public ResponseEntity<ApiResponse<Void>> deleteRecentSearch(@RequestParam String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("삭제할 검색어를 입력해주세요");
        }

        Long userId = getCurrentUserId();
        searchService.deleteRecentSearch(userId, query.trim());

        return ResponseEntity.ok(ApiResponse.success("검색어가 삭제되었습니다"));
    }

    // ✅ 4. 최근 검색어 전체 삭제
    @DeleteMapping("/recent/all")
    public ResponseEntity<ApiResponse<Void>> clearRecentSearches() {
        Long userId = getCurrentUserId();
        searchService.clearRecentSearches(userId);

        return ResponseEntity.ok(ApiResponse.success("모든 검색어가 삭제되었습니다"));
    }

    // ✅ 5. 인기 검색어 조회
    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<List<PopularSearchResponse>>> getPopularSearches() {
        List<PopularSearchResponse> popularSearches = searchService.getPopularSearches();

        return ResponseEntity.ok(ApiResponse.success("인기 검색어를 조회했습니다", popularSearches));
    }
}