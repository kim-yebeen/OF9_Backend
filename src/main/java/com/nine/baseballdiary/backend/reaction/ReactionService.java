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

    // 공감 및 공감 취소 토글
    public void toggleReaction(Long userId, Long recordId, ReactionRequest request) {
        Optional<RecordReaction> existing =
                reactionRepo.findByRecordIdAndUserId(recordId, userId);

        if (existing.isPresent()) {
            handleExistingReaction(existing.get(), request.getReactionTypeId());
        } else {
            createNewReaction(recordId, userId, request);
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

    private void createNewReaction(Long recordId, Long userId, ReactionRequest request) {
        RecordReaction newReaction = RecordReaction.builder()
                .recordId(recordId)
                .userId(userId)
                .reactionTypeId(request.getReactionTypeId())
                .build();
        reactionRepo.save(newReaction);
    }

    // 상위 3개 공감 스티커 + 나머지 개수 반환
    @Transactional(readOnly = true)
    public TopReactionsResponse getTopReactions(Long recordId) {
        List<ReactionStatsResponse> allStats = getStats(recordId);

        // 개수 기준으로 내림차순 정렬
        List<ReactionStatsResponse> sortedStats = allStats.stream()
                .filter(stat -> stat.getCount() > 0)
                .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
                .collect(Collectors.toList());

        // 상위 3개 추출
        List<ReactionStatsResponse> top3 = sortedStats.stream()
                .limit(3)
                .collect(Collectors.toList());

        // 나머지 개수 계산
        int remainingCount = Math.max(0, sortedStats.size() - 3);

        return new TopReactionsResponse(top3, remainingCount);
    }
}
