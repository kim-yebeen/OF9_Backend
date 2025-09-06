package com.nine.baseballdiary.backend.reaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface RecordReactionRepository extends JpaRepository<RecordReaction, Long> {

    Optional<RecordReaction> findByRecordIdAndUserId(Long recordId, Long userId);

    long countByRecordIdAndReactionTypeId(Long recordId, Integer reactionTypeId);

    long countByRecordId(Long recordId);

    @Query("""
        SELECT new com.nine.baseballdiary.backend.reaction.ReactionUserResponse(
            u.id, u.nickname, u.profileImageUrl, u.favTeam, rt.name
        )
        FROM RecordReaction rr
        JOIN User u ON rr.userId = u.id
        JOIN ReactionType rt ON rr.reactionTypeId = rt.displayOrder
        WHERE rr.recordId = :recordId
        ORDER BY rr.createdAt DESC
        """)
    List<ReactionUserResponse> findUsersByRecordId(@Param("recordId") Long recordId);

    @Modifying
    @Query("""
        DELETE FROM RecordReaction rr 
        WHERE rr.userId = :reactorUserId 
        AND rr.recordId IN (
            SELECT r.recordId FROM Record r WHERE r.userId = :recordOwnerId
        )
        """)
    void deleteByReactorAndRecordOwner(@Param("reactorUserId") Long reactorUserId,
                                       @Param("recordOwnerId") Long recordOwnerId);

    // 양방향 리액션 삭제 (차단 시 서로의 리액션 모두 삭제)
    @Modifying
    @Query("""
        DELETE FROM RecordReaction rr 
        WHERE (rr.userId = :userId1 AND rr.recordId IN (
                SELECT r.recordId FROM Record r WHERE r.userId = :userId2
            ))
        OR (rr.userId = :userId2 AND rr.recordId IN (
                SELECT r.recordId FROM Record r WHERE r.userId = :userId1
            ))
        """)
    void deleteByBothUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
}



