package com.nine.baseballdiary.backend.record;

import com.nine.baseballdiary.backend.user.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/records")
@RequiredArgsConstructor
public class RecordController {
    private final RecordService service;

    // 2-1) 1단계 생성 (최소 필수정보)
    @PostMapping
    public ResponseEntity<RecordUploadResponse> uploadRecord(@RequestBody CreateRecordRequest req) {
        RecordUploadResponse res = service.uploadRecord(req);
        return ResponseEntity.status(201).body(res);
    }

    // 2-2) 2단계 수정 (상세 입력)
    @PatchMapping("/{recordId}")
    public ResponseEntity<RecordDetailResponse> updateRecord(
            @PathVariable Long recordId,
            @RequestBody UpdateRecordRequest req
    ) {
        RecordDetailResponse res = service.updateRecord(recordId, req);
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
    @GetMapping("/feed/{userId}")
    public ResponseEntity<List<RecordFeedResponse>> getUserRecordsFeed(@PathVariable Long userId) {
        List<RecordFeedResponse> response = service.getUserRecordsFeed(userId);
        return ResponseEntity.ok(response);
    }

    // 리스트 형식으로 직관 기록 조회
    @GetMapping("/list/{userId}")
    public ResponseEntity<List<RecordListResponse>> getUserRecordsList(@PathVariable Long userId) {
        List<RecordListResponse> response = service.getUserRecordsList(userId);
        return ResponseEntity.ok(response);
    }

    // 캘린더 형식으로 직관 기록 조회
    @GetMapping("/calendar/{userId}")
    public ResponseEntity<List<RecordCalendarResponse>> getUserRecordsCalendar(@PathVariable Long userId) {
        List<RecordCalendarResponse> response = service.getUserRecordsCalendar(userId);
        return ResponseEntity.ok(response);
    }

    //레코드 삭제
    @DeleteMapping("/{recordId}")
    public ResponseEntity<Void> deleteRecord(@PathVariable Long recordId) {
        service.deleteRecord(recordId);
        return ResponseEntity.noContent().build();
    }

    // 함께한 사람(맞팔+검색) 불러오기 API
    @GetMapping("/{userId}/mutual-friends")
    public List<UserDto> getMutualFriends(
            @PathVariable Long userId,
            @RequestParam(required = false) String query
    ) {
        return service.getMutualFriends(userId, query);
    }
}
