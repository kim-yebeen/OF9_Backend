package com.nine.baseballdiary.backend.reaction;

import com.nine.baseballdiary.backend.record.RecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReactionService {

    private final RecordReactionRepository reactionRepo;
    private final ReactionTypeRepository reactionTypeRepo;
    private final RecordRepository recordRepo;

    // 리액션 추가/변경
    @Transactional
    public void addOrUpdateReaction(Long recordId, AddReactionRequest request) {
        // 레코드 존재 확인
        if (!recordRepo.existsById(recordId)) {
            throw new IllegalArgumentException("존재하지 않는 레코드 ID: " + recordId);
        }

        // 리액션 타입 존재 확인
        if (!reactionTypeRepo.existsById(request.getReactionTypeId())) {
            throw new IllegalArgumentException("존재하지 않는 리액션 타입 ID: " + request.getReactionTypeId());
        }

        // 기존 리액션 확인
        Optional<RecordReaction> existingReaction =
                reactionRepo.findByRecordIdAndUserId(recordId, request.getUserId());

        if (existingReaction.isPresent()) {
            // 같은 리액션이면 삭제, 다른 리액션이면 수정
            RecordReaction existing = existingReaction.get();
            if (existing.getReactionTypeId().equals(request.getReactionTypeId())) {
                reactionRepo.delete(existing);
            } else {
                reactionRepo.delete(existing);
                RecordReaction newReaction = RecordReaction.builder()
                        .recordId(recordId)
                        .userId(request.getUserId())
                        .reactionTypeId(request.getReactionTypeId())
                        .build();
                reactionRepo.save(newReaction);
            }
        } else {
            // 새 리액션 추가
            RecordReaction newReaction = RecordReaction.builder()
                    .recordId(recordId)
                    .userId(request.getUserId())
                    .reactionTypeId(request.getReactionTypeId())
                    .build();
            reactionRepo.save(newReaction);
        }
    }

    // 리액션 삭제
    @Transactional
    public void removeReaction(Long recordId, Long userId) {
        RecordReaction reaction = reactionRepo.findByRecordIdAndUserId(recordId, userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 리액션이 존재하지 않습니다"));
        reactionRepo.delete(reaction);
    }

    // 레코드의 리액션 통계 조회
    @Transactional(readOnly = true)
    public RecordReactionSummary getRecordReactions(Long recordId, Long currentUserId) {
        // 리액션 통계
        List<Object[]> stats = reactionRepo.findReactionStatsByRecordId(recordId);

        // 내가 누른 리액션 확인
        Optional<RecordReaction> myReaction = currentUserId != null ?
                reactionRepo.findByRecordIdAndUserId(recordId, currentUserId) : Optional.empty();
        Long myReactionTypeId = myReaction.map(RecordReaction::getReactionTypeId).orElse(null);

        List<ReactionResponse> reactions = stats.stream()
                .map(stat -> new ReactionResponse(
                        (String) stat[0],  // category
                        (String) stat[1],  // name
                        (String) stat[2],  // emoji
                        ((Number) stat[3]).intValue(), // count
                        myReactionTypeId != null && reactionTypeRepo.findById(myReactionTypeId)
                                .map(rt -> rt.getName().equals(stat[1])).orElse(false)
                ))
                .collect(Collectors.toList());

        // 리액션한 사용자들 정보
        List<ReactionUserInfo> users = reactionRepo.findReactionUsersByRecordId(recordId);

        int totalCount = reactions.stream().mapToInt(ReactionResponse::getCount).sum();

        return new RecordReactionSummary(recordId, reactions, users, totalCount);
    }

    // 모든 리액션 타입 조회
    @Transactional(readOnly = true)
    public List<ReactionType> getAllReactionTypes() {
        return reactionTypeRepo.findAllByOrderByDisplayOrder();
    }
}