package com.nine.baseballdiary.backend.reaction;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/reactions")
@RequiredArgsConstructor
public class ReactionController {

    private final ReactionService reactionService;

    //jwt 토큰에서 현재 사용자 id를 가져오는 헬퍼 메서드
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증된 사용자가 아닙니다.");
        }
        return Long.parseLong((String) authentication.getPrincipal());
    }


    @GetMapping("/types")
    public ResponseEntity<List<ReactionTypeResponse>> getTypes() {
        return ResponseEntity.ok(reactionService.getAllTypes());
    }

    //공감 및 공감 취소
    @PostMapping("/records/{recordId}")
    public ResponseEntity<Void> toggleReaction(
            @PathVariable Long recordId,
            @RequestBody ReactionRequest request) {
        Long userId = getCurrentUserId();
        reactionService.toggleReaction(userId, recordId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/records/{recordId}/stats")
    public ResponseEntity<List<ReactionStatsResponse>> getStats(
            @PathVariable Long recordId) {
        return ResponseEntity.ok(reactionService.getStats(recordId));
    }

    @GetMapping("/records/{recordId}/users")
    public ResponseEntity<List<ReactionUserResponse>> getUsers(
            @PathVariable Long recordId) {
        return ResponseEntity.ok(reactionService.getUsers(recordId));
    }

    @GetMapping("/records/{recordId}/summary")
    public ResponseEntity<RecordReactionSummary> getSummary(
            @PathVariable Long recordId) {
        return ResponseEntity.ok(reactionService.getSummary(recordId));
    }
}
