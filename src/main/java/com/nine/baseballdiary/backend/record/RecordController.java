package com.nine.baseballdiary.backend.record;

import com.nine.baseballdiary.backend.user.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/records")
@RequiredArgsConstructor
public class RecordController {
    private final RecordService service;

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증된 사용자가 아닙니다.");
        }
        return Long.parseLong((String) authentication.getPrincipal());
    }

    @PostMapping
    public ResponseEntity<RecordUploadResponse> upload(@RequestBody CreateRecordRequest req) {
        Long userId = getCurrentUserId();
        RecordUploadResponse res = service.uploadRecord(userId, req);
        return ResponseEntity.status(201).body(res);
    }

    // 레코드 수정
    @PatchMapping("/{recordId}")
    public ResponseEntity<RecordDetailResponse> updateRecord(
            @PathVariable Long recordId,
            @RequestBody UpdateRecordRequest req
    ) {
        Long userId = getCurrentUserId();
        RecordDetailResponse res = service.updateRecord(userId, recordId, req);
        return ResponseEntity.ok(res);
    }

    // 상세 정보 페이지에 표시될 모든 정보 (새로운 details 엔드포인트)
    @GetMapping("/{recordId}/details")
    public ResponseEntity<RecordDetailResponse> getRecordDetail(@PathVariable Long recordId){
        // Record 상세 정보 요청
        RecordDetailResponse res = service.getRecordDetail(recordId);
        return ResponseEntity.status(200).body(res);
    }

    // 피드 형식으로 직관 기록 조회
    @GetMapping("/me/feed")
    public ResponseEntity<List<RecordFeedResponse>> getUserRecordsFeed() {
        Long userId = getCurrentUserId();
        List<RecordFeedResponse> response = service.getUserRecordsFeed(userId);
        return ResponseEntity.ok(response);
    }

    // 리스트 형식으로 직관 기록 조회
    @GetMapping("/me/list")
    public ResponseEntity<List<RecordListResponse>> getUserRecordsList() {
        Long userId = getCurrentUserId();
        List<RecordListResponse> response = service.getUserRecordsList(userId);
        return ResponseEntity.ok(response);
    }

    // 캘린더 형식으로 직관 기록 조회
    @GetMapping("/me/calendar")
    public ResponseEntity<List<RecordCalendarResponse>> getUserRecordsCalendar() {
        Long userId = getCurrentUserId();
        List<RecordCalendarResponse> response = service.getUserRecordsCalendar(userId);
        return ResponseEntity.ok(response);
    }

    //레코드 삭제
    @DeleteMapping("/{recordId}")
    public ResponseEntity<Void> deleteRecord(@PathVariable Long recordId) {
        Long userId = getCurrentUserId();
        service.deleteRecord(userId, recordId);
        return ResponseEntity.noContent().build();
    }

    // 함께한 사람(맞팔+검색) 불러오기 API
    @GetMapping("/me/mutual-friends")
    public List<UserDto> getMutualFriends(
            @RequestParam(required = false) String query
    ) {
        Long userId = getCurrentUserId();
        return service.getMutualFriends(userId, query);
    }
}
