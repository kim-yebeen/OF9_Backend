package com.nine.baseballdiary.backend.reaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface RecordReactionRepository extends JpaRepository<RecordReaction, Long> {

    Optional<RecordReaction> findByRecordIdAndUserId(Long recordId, Long userId);

    List<RecordReaction> findByRecordId(Long recordId);

    @Query("""
        SELECT new com.nine.baseballdiary.backend.reaction.ReactionUserInfo(
            u.id, u.nickname, u.profileImageUrl, u.favTeam, rt.name, rt.emoji
        )
        FROM RecordReaction rr
        JOIN User u ON rr.userId = u.id
        JOIN ReactionType rt ON rr.reactionTypeId = rt.id
        WHERE rr.recordId = :recordId
        ORDER BY rr.createdAt DESC
    """)
    List<ReactionUserInfo> findReactionUsersByRecordId(@Param("recordId") Long recordId);

    @Query("""
        SELECT rt.category, rt.name, rt.emoji, COUNT(rr) as count
        FROM RecordReaction rr
        JOIN ReactionType rt ON rr.reactionTypeId = rt.id
        WHERE rr.recordId = :recordId
        GROUP BY rt.id, rt.category, rt.name, rt.emoji, rt.displayOrder
        ORDER BY rt.displayOrder
    """)
    List<Object[]> findReactionStatsByRecordId(@Param("recordId") Long recordId);
}