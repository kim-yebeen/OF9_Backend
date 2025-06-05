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

    @GetMapping("/types")
    public ResponseEntity<List<ReactionTypeResponse>> getTypes() {
        return ResponseEntity.ok(reactionService.getAllTypes());
    }

    @PostMapping("/records/{recordId}")
    public ResponseEntity<Void> toggleReaction(
            @PathVariable Long recordId,
            @RequestBody ReactionRequest request) {
        reactionService.toggleReaction(recordId, request);
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
