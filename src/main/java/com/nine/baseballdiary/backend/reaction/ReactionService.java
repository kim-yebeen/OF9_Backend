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
@Transactional
public class ReactionService {

    private final RecordReactionRepository reactionRepo;
    private final ReactionTypeRepository reactionTypeRepo;

    // 15가지 타입 목록 조회
    @Transactional(readOnly = true)
    public List<ReactionTypeResponse> getAllTypes() {
        return reactionTypeRepo.findAllByOrderByDisplayOrder()
                .stream()
                .map(type -> new ReactionTypeResponse(
                        type.getDisplayOrder(),
                        type.getCategory(),
                        type.getName()
                ))
                .collect(Collectors.toList());
    }


    // 사용자 목록 조회 (getUsers 메서드)
    @Transactional(readOnly = true)
    public List<ReactionUserResponse> getUsers(Long recordId) {
        return reactionRepo.findUsersByRecordId(recordId);
    }

    // getSummary() 메서드 추가
    @Transactional(readOnly = true)
    public RecordReactionSummary getSummary(Long recordId) {
        List<ReactionStatsResponse> stats = getStats(recordId);
        Integer totalCount = getTotalCount(recordId);
        return new RecordReactionSummary(stats, totalCount);
    }

    // getStats() 메서드 추가
    @Transactional(readOnly = true)
    public List<ReactionStatsResponse> getStats(Long recordId) {
        return reactionTypeRepo.findAllByOrderByDisplayOrder()
                .stream()
                .map(type -> {
                    long count = reactionRepo.countByRecordIdAndReactionTypeId(
                            recordId, type.getDisplayOrder());
                    return new ReactionStatsResponse(type.getName(), count);
                })
                .filter(stats -> stats.getCount() > 0)
                .collect(Collectors.toList());
    }

    // getTotalCount() 메서드 추가
    @Transactional(readOnly = true)
    public Integer getTotalCount(Long recordId) {
        return Math.toIntExact(reactionRepo.countByRecordId(recordId));
    }

    // 기존 toggleReaction 메서드...
    public void toggleReaction(Long recordId, ReactionRequest request) {
        Optional<RecordReaction> existing =
                reactionRepo.findByRecordIdAndUserId(recordId, request.getUserId());

        if (existing.isPresent()) {
            handleExistingReaction(existing.get(), request.getReactionTypeId());
        } else {
            createNewReaction(recordId, request);
        }
    }

    // Private helper methods
    private void handleExistingReaction(RecordReaction existing, Integer newReactionTypeId) {
        if (existing.getReactionTypeId().equals(newReactionTypeId)) {
            reactionRepo.delete(existing);
        } else {
            existing.updateReactionType(newReactionTypeId);
        }
    }

    private void createNewReaction(Long recordId, ReactionRequest request) {
        RecordReaction newReaction = RecordReaction.builder()
                .recordId(recordId)
                .userId(request.getUserId())
                .reactionTypeId(request.getReactionTypeId())
                .build();
        reactionRepo.save(newReaction);
    }

}
