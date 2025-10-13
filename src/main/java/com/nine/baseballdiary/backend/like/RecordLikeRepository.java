package com.nine.baseballdiary.backend.like;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RecordLikeRepository extends JpaRepository<RecordLike, Long> {

    // 특정 게시물에 대한 좋아요 여부 확인
    boolean existsByRecordIdAndUserId(Long recordId, Long userId);

    // 특정 게시물에 대한 특정 사용자의 좋아요 조회
    Optional<RecordLike> findByRecordIdAndUserId(Long recordId, Long userId);

    // 특정 게시물의 총 좋아요 개수
    long countByRecordId(Long recordId);

    // 특정 게시물의 좋아요를 누른 사용자 목록 (프로필 정보 포함)
    @Query("""
        SELECT new com.nine.baseballdiary.backend.like.LikeUserResponse(
            u.id, u.nickname, u.profileImageUrl, u.favTeam, rl.createdAt
        )
        FROM RecordLike rl
        JOIN User u ON rl.userId = u.id
        WHERE rl.recordId = :recordId
        ORDER BY rl.createdAt DESC
    """)
    List<LikeUserResponse> findLikeUsersByRecordId(@Param("recordId") Long recordId);

    // ✅ UserService에서 사용하는 메서드들 추가
    @Modifying
    @Query("DELETE FROM RecordLike rl WHERE rl.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("""
        DELETE FROM RecordLike rl 
        WHERE (rl.userId = :userId1 AND rl.recordId IN (
            SELECT r.recordId FROM GameRecord r WHERE r.userId = :userId2
        )) OR (rl.userId = :userId2 AND rl.recordId IN (
            SELECT r.recordId FROM GameRecord r WHERE r.userId = :userId1
        ))
    """)
    void deleteByBothUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
}