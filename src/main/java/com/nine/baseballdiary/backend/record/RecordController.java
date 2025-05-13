package com.nine.baseballdiary.backend.record;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
