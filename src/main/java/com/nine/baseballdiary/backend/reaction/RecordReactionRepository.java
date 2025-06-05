package com.nine.baseballdiary.backend.reaction;

import org.springframework.data.jpa.repository.JpaRepository;
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


}

