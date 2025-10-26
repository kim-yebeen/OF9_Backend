package com.nine.baseballdiary.backend.record;

import com.nine.baseballdiary.backend.common.response.ApiResponse;
import com.nine.baseballdiary.backend.user.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/records")
@RequiredArgsConstructor
public class RecordController {
    private final RecordService service;

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("인증되지 않은 사용자입니다");
        }

        if ("anonymousUser".equals(authentication.getName())) {
            throw new IllegalStateException("로그인이 필요한 서비스입니다");
        }

        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("올바르지 않은 토큰입니다");
        }
    }

    // 직관 기록 등록
    @PostMapping
    public ResponseEntity<ApiResponse<RecordUploadResponse>> uploadRecord(
            @RequestBody CreateRecordRequest req) {
        try {
            Long userId = getCurrentUserId();
            RecordUploadResponse response = service.uploadRecord(userId, req);

            return ResponseEntity.ok(ApiResponse.success("직관 기록이 성공적으로 등록되었습니다", response));

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage(), "UNAUTHORIZED"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), "BAD_REQUEST"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("직관 기록 등록 중 서버 오류가 발생했습니다", "INTERNAL_SERVER_ERROR"));
        }
    }

    // 레코드 수정
    @PatchMapping("/{recordId}")
    public ResponseEntity<ApiResponse<RecordDetailResponse>> updateRecord(
            @PathVariable Long recordId,
            @RequestBody UpdateRecordRequest req) {
        try {
            Long userId = getCurrentUserId();
            RecordDetailResponse response = service.updateRecord(userId, recordId, req);

            return ResponseEntity.ok(ApiResponse.success("직관 기록이 성공적으로 수정되었습니다", response));

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage(), "UNAUTHORIZED"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), "BAD_REQUEST"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("직관 기록 수정 중 서버 오류가 발생했습니다", "INTERNAL_SERVER_ERROR"));
        }
    }

    // 상세 정보 페이지에 표시될 모든 정보 (로그인한 사용자 정보 포함)
    @GetMapping("/{recordId}/details")
    public ResponseEntity<ApiResponse<RecordDetailResponse>> getRecordDetail(@PathVariable Long recordId) {
        try {
            // ✅ [수정] 현재 로그인한 사용자 ID를 가져와 서비스에 전달합니다.
            Long currentUserId = getCurrentUserId();
            RecordDetailResponse response = service.getRecordDetailWithUser(recordId, currentUserId);

            return ResponseEntity.ok(ApiResponse.success("직관 기록 상세 정보를 성공적으로 조회했습니다", response));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage(), "NOT_FOUND"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("직관 기록 조회 중 서버 오류가 발생했습니다", "INTERNAL_SERVER_ERROR"));
        }
    }

    // 공개용 레코드 상세 조회 (로그인 없이 접근 가능)
    @GetMapping("/{recordId}/public")
    public ResponseEntity<ApiResponse<RecordDetailResponse>> getPublicRecordDetail(@PathVariable Long recordId) {
        try {
            RecordDetailResponse response = service.getRecordDetail(recordId);

            return ResponseEntity.ok(ApiResponse.success("직관 기록 상세 정보를 성공적으로 조회했습니다", response));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage(), "NOT_FOUND"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("직관 기록 조회 중 서버 오류가 발생했습니다", "INTERNAL_SERVER_ERROR"));
        }
    }

    // 피드 형식으로 직관 기록 조회
    @GetMapping("/me/feed")
    public ResponseEntity<ApiResponse<List<RecordFeedResponse>>> getUserRecordsFeed() {
        try {
            Long userId = getCurrentUserId();
            List<RecordFeedResponse> response = service.getUserRecordsFeed(userId);

            return ResponseEntity.ok(ApiResponse.success("피드 형식 직관 기록을 성공적으로 조회했습니다", response));

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage(), "UNAUTHORIZED"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("피드 조회 중 서버 오류가 발생했습니다", "INTERNAL_SERVER_ERROR"));
        }
    }

    // 리스트 형식으로 직관 기록 조회
    @GetMapping("/me/list")
    public ResponseEntity<ApiResponse<List<RecordListResponse>>> getUserRecordsList() {
        try {
            Long userId = getCurrentUserId();
            // (userId) -> (userId, userId)로 변경
            List<RecordListResponse> response = service.getUserRecordsList(userId, userId);
            return ResponseEntity.ok(ApiResponse.success("리스트 형식 직관 기록을 성공적으로 조회했습니다", response));

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage(), "UNAUTHORIZED"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("리스트 조회 중 서버 오류가 발생했습니다", "INTERNAL_SERVER_ERROR"));
        }
    }

    // 캘린더 형식으로 직관 기록 조회
    @GetMapping("/me/calendar")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserRecordsCalendar(
            @RequestParam(defaultValue = "2025") int year,
            @RequestParam(defaultValue = "10") int month) {
        try {
            Long userId = getCurrentUserId();
            // (userId, year, month) -> (userId, year, month) (파라미터 이름만 변경됨)
            Map<String, Object> response = service.getUserRecordsCalendar(userId, year, month);
            return ResponseEntity.ok(ApiResponse.success("캘린더 형식 직관 기록을 성공적으로 조회했습니다", response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("캘린더 조회 중 서버 오류가 발생했습니다", "INTERNAL_SERVER_ERROR"));
        }
    }

    // 레코드 삭제
    @DeleteMapping("/{recordId}")
    public ResponseEntity<ApiResponse<Void>> deleteRecord(@PathVariable Long recordId) {
        try {
            Long userId = getCurrentUserId();
            service.deleteRecord(userId, recordId);

            return ResponseEntity.ok(ApiResponse.success("직관 기록이 성공적으로 삭제되었습니다"));

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage(), "UNAUTHORIZED"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), "BAD_REQUEST"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("직관 기록 삭제 중 서버 오류가 발생했습니다", "INTERNAL_SERVER_ERROR"));
        }
    }

    // 함께한 사람(맞팔+검색) 불러오기 API
    @GetMapping("/me/mutual-friends")
    public ResponseEntity<ApiResponse<List<UserDto>>> getMutualFriends(
            @RequestParam(required = false) String query) {
        try {
            Long userId = getCurrentUserId();
            List<UserDto> response = service.getMutualFriends(userId, query);

            return ResponseEntity.ok(ApiResponse.success("맞팔 친구 목록을 성공적으로 조회했습니다", response));

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage(), "UNAUTHORIZED"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("맞팔 친구 조회 중 서버 오류가 발생했습니다", "INTERNAL_SERVER_ERROR"));
        }
    }
}