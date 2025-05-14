package com.nine.baseballdiary.backend.record;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/records")
@RequiredArgsConstructor
public class RecordController {
    private final RecordService service;

    @PostMapping
    public ResponseEntity<RecordResponse> create(@RequestBody CreateRecordRequest req) {
        // Record 생성 요청
        RecordResponse res = service.createRecord(req);
        return ResponseEntity.status(201).body(res);
    }

    //
    @GetMapping("/{recordId}/details")
    public ResponseEntity<RecordDetailResponse> getRecordDetail(@PathVariable Long recordId){
        //Record 상세 정보 요청
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
}
