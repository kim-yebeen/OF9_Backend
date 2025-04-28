package com.nine.baseballdiary.backend.record;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/records")
@RequiredArgsConstructor
public class RecordController {

    private final RecordService service;

    /** 1) Draft 생성 */
    @PostMapping
    public ResponseEntity<RecordResponse> createDraft(
            @Valid @RequestBody CreateDraftRequest req
    ) {
        Integer mockUserId = 1; // TODO: JWT에서 실제 userId 추출
        return ResponseEntity.status(201)
                .body(service.createDraft(mockUserId, req));
    }

    /** 2) Draft 조회 */
    @GetMapping("/{id}")
    public ResponseEntity<RecordResponse> getRecord(@PathVariable Integer id) {
        return ResponseEntity.ok(service.getRecord(id));
    }

    /** 3) Draft 보정 */
    @PatchMapping("/{id}")
    public ResponseEntity<RecordResponse> updateDraft(
            @PathVariable Integer id,
            @RequestBody UpdateDraftRequest req
    ) {
        return ResponseEntity.ok(service.updateDraft(id, req));
    }

    /** 4) 세부 입력 저장 */
    @PostMapping("/{id}/detail")
    public ResponseEntity<RecordResponse> completeDetail(
            @PathVariable Integer id,
            @Valid @RequestBody CreateDetailRequest req
    ) {
        return ResponseEntity.ok(service.completeDetail(id, req));
    }
}
