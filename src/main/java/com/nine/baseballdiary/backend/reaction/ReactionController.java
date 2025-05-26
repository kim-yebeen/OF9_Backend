package com.nine.baseballdiary.backend.reaction;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/reactions")
@RequiredArgsConstructor
public class ReactionController {

    private final ReactionService reactionService;

    // 리액션 추가/변경/삭제 (토글 방식)
    @PostMapping("/records/{recordId}")
    public ResponseEntity<Void> toggleReaction(
            @PathVariable Long recordId,
            @RequestBody AddReactionRequest request
    ) {
        reactionService.addOrUpdateReaction(recordId, request);
        return ResponseEntity.ok().build();
    }

    // 리액션 삭제 (명시적)
    @DeleteMapping("/records/{recordId}/users/{userId}")
    public ResponseEntity<Void> removeReaction(
            @PathVariable Long recordId,
            @PathVariable Long userId
    ) {
        reactionService.removeReaction(recordId, userId);
        return ResponseEntity.noContent().build();
    }

    // 레코드의 리액션 통계 및 사용자 목록 조회
    @GetMapping("/records/{recordId}")
    public ResponseEntity<RecordReactionSummary> getRecordReactions(
            @PathVariable Long recordId,
            @RequestParam(required = false) Long currentUserId
    ) {
        RecordReactionSummary summary = reactionService.getRecordReactions(recordId, currentUserId);
        return ResponseEntity.ok(summary);
    }

    // 모든 리액션 타입 조회 (프론트엔드에서 리액션 버튼 렌더링용)
    @GetMapping("/types")
    public ResponseEntity<List<ReactionType>> getAllReactionTypes() {
        List<ReactionType> types = reactionService.getAllReactionTypes();
        return ResponseEntity.ok(types);
    }
}