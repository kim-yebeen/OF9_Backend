package com.nine.baseballdiary.backend.record;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/records")
@RequiredArgsConstructor
public class RecordController {

    private final RecordService svc;


    // 1) 티켓 업로드
    @PostMapping("/upload")
    public ResponseEntity<UploadRecordResponse> upload(
            @RequestParam("file") MultipartFile file
    ) throws Exception {
        Integer mockUserId = 1; // 실제 인증 후 userId를 꺼내오세요
        return ResponseEntity.status(201).body(svc.uploadTicket(mockUserId, file));
    }

    // 2) 조회
    @GetMapping("/{id}")
    public RecordResponse get(@PathVariable Integer id) {
        return svc.getRecord(id);
    }

    // 3) Draft 수정
    @PatchMapping("/{id}")
    public RecordResponse patch(
            @PathVariable Integer id,
            @RequestBody UpdateDraftRequest req
    ) {
        return svc.updateDraft(id, req);
    }

    // 4) Detail 저장
    @PostMapping("/{id}/detail")
    public ResponseEntity<RecordResponse> detail(
            @PathVariable Integer id,
            @Valid @RequestBody CreateDetailRequest req  // ← @Valid 추가
    ) {
        return ResponseEntity.ok(svc.completeDetail(id, req));
    }
}
