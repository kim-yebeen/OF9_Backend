package com.nine.baseballdiary.backend.record;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface GameRecordRepository extends JpaRepository<GameRecord, Long> {

    // ✅ 전체 피드 조회 (다중 필터링 적용)
    @Query("""
    SELECT r FROM GameRecord r 
    JOIN Game g ON r.game.gameId = g.gameId 
    JOIN User u ON r.userId = u.id 
    WHERE (
        u.isPrivate = false OR 
        r.userId = :currentUserId OR 
        r.userId IN :followingIds
    )
    AND (:team IS NULL OR g.homeTeam = :team OR g.awayTeam = :team)
    AND (:stadium IS NULL OR r.stadium = :stadium)
    AND (:seatInfo IS NULL OR r.seatInfo LIKE CONCAT('%', :seatInfo, '%'))
    AND (cast(:date as date) IS NULL OR g.date = :date)
    AND NOT EXISTS (
        SELECT 1 FROM UserBlock ub 
        WHERE (ub.blocker.id = :currentUserId AND ub.blocked.id = r.userId)
           OR (ub.blocker.id = r.userId AND ub.blocked.id = :currentUserId)
    )
    ORDER BY r.createdAt DESC
    """)
    List<GameRecord> findAllFeedRecordsWithFilters(
            @Param("currentUserId") Long currentUserId,
            @Param("followingIds") List<Long> followingIds,
            @Param("team") String team,
            @Param("stadium") String stadium,
            @Param("seatInfo") String seatInfo,
            @Param("date") LocalDate date,
            Pageable pageable
    );

    // ✅ 팔로잉 피드 조회 (다중 필터링 적용)
    @Query("""
    SELECT r FROM GameRecord r 
    JOIN Game g ON r.game.gameId = g.gameId 
    WHERE r.userId IN :userIds
    AND (:team IS NULL OR g.homeTeam = :team OR g.awayTeam = :team)
    AND (:stadium IS NULL OR r.stadium = :stadium)
    AND (:seatInfo IS NULL OR r.seatInfo LIKE CONCAT('%', :seatInfo, '%'))
    AND (cast(:date as date) IS NULL OR g.date = :date)
    AND NOT EXISTS (
        SELECT 1 FROM UserBlock ub 
        WHERE (ub.blocker.id = :currentUserId AND ub.blocked.id = r.userId)
           OR (ub.blocker.id = r.userId AND ub.blocked.id = :currentUserId)
    )
    ORDER BY r.createdAt DESC
    """)
    List<GameRecord> findFollowingFeedRecordsWithFilters(
            @Param("userIds") List<Long> userIds,
            @Param("currentUserId") Long currentUserId,
            @Param("team") String team,
            @Param("stadium") String stadium,
            @Param("seatInfo") String seatInfo,
            @Param("date") LocalDate date,
            Pageable pageable
    );

    // ✅ 전체 피드 - 최신순 (날짜 필터 제거, 팀 필터만)
    @Query("""
    SELECT r FROM GameRecord r 
    JOIN Game g ON r.game.gameId = g.gameId 
    JOIN User u ON r.userId = u.id 
    WHERE (
        u.isPrivate = false OR 
        r.userId = :currentUserId OR 
        r.userId IN :followingIds
    )
    AND (:team IS NULL OR g.homeTeam = :team OR g.awayTeam = :team)
    ORDER BY r.createdAt DESC
    """)
    List<GameRecord> findAllFeedRecords(
            @Param("currentUserId") Long currentUserId,
            @Param("followingIds") List<Long> followingIds,
            @Param("team") String team,
            Pageable pageable
    );

    // ✅ 전체 피드 - 차단 필터 포함
    @Query("""
    SELECT r FROM GameRecord r 
    JOIN Game g ON r.game.gameId = g.gameId 
    JOIN User u ON r.userId = u.id 
    WHERE (
        u.isPrivate = false OR 
        r.userId = :currentUserId OR 
        r.userId IN :followingIds
    )
    AND (:team IS NULL OR g.homeTeam = :team OR g.awayTeam = :team)
    AND NOT EXISTS (
        SELECT 1 FROM UserBlock ub 
        WHERE (ub.blocker.id = :currentUserId AND ub.blocked.id = r.userId)
           OR (ub.blocker.id = r.userId AND ub.blocked.id = :currentUserId)
    )
    ORDER BY r.createdAt DESC
    """)
    List<GameRecord> findAllFeedRecordsWithBlockFilter(
            @Param("currentUserId") Long currentUserId,
            @Param("followingIds") List<Long> followingIds,
            @Param("team") String team,
            Pageable pageable
    );

    // ✅ 팔로잉 피드 - 최신순
    @Query("""
    SELECT r FROM GameRecord r 
    JOIN Game g ON r.game.gameId = g.gameId 
    WHERE r.userId IN :userIds
    AND (:team IS NULL OR g.homeTeam = :team OR g.awayTeam = :team)
    ORDER BY r.createdAt DESC
    """)
    List<GameRecord> findFollowingFeedRecords(
            @Param("userIds") List<Long> userIds,
            @Param("team") String team,
            Pageable pageable
    );

    // ✅ 팔로잉 피드 - 차단 필터 포함
    @Query("""
    SELECT r FROM GameRecord r 
    JOIN Game g ON r.game.gameId = g.gameId 
    WHERE r.userId IN :userIds
    AND (:team IS NULL OR g.homeTeam = :team OR g.awayTeam = :team)
    AND NOT EXISTS (
        SELECT 1 FROM UserBlock ub 
        WHERE (ub.blocker.id = :currentUserId AND ub.blocked.id = r.userId)
           OR (ub.blocker.id = r.userId AND ub.blocked.id = :currentUserId)
    )
    ORDER BY r.createdAt DESC
    """)
    List<GameRecord> findFollowingFeedRecordsWithBlockFilter(
            @Param("userIds") List<Long> userIds,
            @Param("currentUserId") Long currentUserId,
            @Param("team") String team,
            Pageable pageable
    );

    // ✅ 기존 메서드들 (마이페이지, 검색 등)
    long countByUserId(Long userId);

    List<GameRecord> findByUserId(Long userId);

    @Query(value = """
    SELECT r.*, 
           (CASE 
               WHEN LOWER(r.comment) LIKE CONCAT('%', :query, '%') THEN 3
               WHEN LOWER(r.long_content) LIKE CONCAT('%', :query, '%') THEN 2
               WHEN LOWER(r.best_player) LIKE CONCAT('%', :query, '%') THEN 1
               WHEN LOWER(u.nickname) LIKE CONCAT('%', :query, '%') THEN 2
               ELSE 0
           END +
           COALESCE((LENGTH(r.comment) - LENGTH(REPLACE(LOWER(r.comment), :query, ''))) / LENGTH(:query) * 0.3, 0) +
           COALESCE((LENGTH(r.long_content) - LENGTH(REPLACE(LOWER(r.long_content), :query, ''))) / LENGTH(:query) * 0.2, 0) +
           COALESCE((LENGTH(r.best_player) - LENGTH(REPLACE(LOWER(r.best_player), :query, ''))) / LENGTH(:query) * 0.1, 0)
           ) as relevance_score
    FROM record r
    JOIN users u ON r.user_id = u.id
    WHERE (
        (u.is_private = false) OR 
        (u.is_private = true AND u.id = ANY(CAST(:followingUserIds AS bigint[]))) OR
        (u.id = :currentUserId)
    )
    AND (
        LOWER(r.comment) LIKE CONCAT('%', :query, '%') OR
        LOWER(r.long_content) LIKE CONCAT('%', :query, '%') OR
        LOWER(r.best_player) LIKE CONCAT('%', :query, '%') OR
        LOWER(u.nickname) LIKE CONCAT('%', :query, '%')
    )
    ORDER BY relevance_score DESC, r.created_at DESC
    """,
            countQuery = """
    SELECT COUNT(*)
    FROM record r
    JOIN users u ON r.user_id = u.id
    WHERE (
        (u.is_private = false) OR 
        (u.is_private = true AND u.id = ANY(CAST(:followingUserIds AS bigint[]))) OR
        (u.id = :currentUserId)
    )
    AND (
        LOWER(r.comment) LIKE CONCAT('%', :query, '%') OR
        LOWER(r.long_content) LIKE CONCAT('%', :query, '%') OR
        LOWER(r.best_player) LIKE CONCAT('%', :query, '%') OR
        LOWER(u.nickname) LIKE CONCAT('%', :query, '%')
    )
    """,
            nativeQuery = true)
    Page<GameRecord> searchRecordsWithAccess(
            @Param("query") String query,
            @Param("currentUserId") Long currentUserId,
            @Param("followingUserIds") String followingUserIds,
            Pageable pageable
    );

    @Query(value = """
    SELECT r.*, 
           (CASE 
               WHEN LOWER(r.comment) LIKE CONCAT('%', :query, '%') THEN 3
               WHEN LOWER(r.long_content) LIKE CONCAT('%', :query, '%') THEN 2
               WHEN LOWER(r.best_player) LIKE CONCAT('%', :query, '%') THEN 1
               WHEN LOWER(u.nickname) LIKE CONCAT('%', :query, '%') THEN 2
               ELSE 0
           END +
           COALESCE((LENGTH(r.comment) - LENGTH(REPLACE(LOWER(r.comment), :query, ''))) / LENGTH(:query) * 0.3, 0) +
           COALESCE((LENGTH(r.long_content) - LENGTH(REPLACE(LOWER(r.long_content), :query, ''))) / LENGTH(:query) * 0.2, 0) +
           COALESCE((LENGTH(r.best_player) - LENGTH(REPLACE(LOWER(r.best_player), :query, ''))) / LENGTH(:query) * 0.1, 0)
           ) as relevance_score
    FROM record r
    JOIN users u ON r.user_id = u.id
    WHERE (
        (u.is_private = false) OR 
        (u.is_private = true AND u.id = ANY(CAST(:followingUserIds AS bigint[]))) OR
        (u.id = :currentUserId)
    )
    AND (
        LOWER(r.comment) LIKE CONCAT('%', :query, '%') OR
        LOWER(r.long_content) LIKE CONCAT('%', :query, '%') OR
        LOWER(r.best_player) LIKE CONCAT('%', :query, '%') OR
        LOWER(u.nickname) LIKE CONCAT('%', :query, '%')
    )
    AND NOT EXISTS (
        SELECT 1 FROM user_block ub 
        WHERE (ub.blocker_id = :currentUserId AND ub.blocked_id = r.user_id)
           OR (ub.blocker_id = r.user_id AND ub.blocked_id = :currentUserId)
    )
    ORDER BY relevance_score DESC, r.created_at DESC
    """,
            countQuery = """
    SELECT COUNT(*)
    FROM record r
    JOIN users u ON r.user_id = u.id
    WHERE (
        (u.is_private = false) OR 
        (u.is_private = true AND u.id = ANY(CAST(:followingUserIds AS bigint[]))) OR
        (u.id = :currentUserId)
    )
    AND (
        LOWER(r.comment) LIKE CONCAT('%', :query, '%') OR
        LOWER(r.long_content) LIKE CONCAT('%', :query, '%') OR
        LOWER(r.best_player) LIKE CONCAT('%', :query, '%') OR
        LOWER(u.nickname) LIKE CONCAT('%', :query, '%')
    )
    AND NOT EXISTS (
        SELECT 1 FROM user_block ub 
        WHERE (ub.blocker_id = :currentUserId AND ub.blocked_id = r.user_id)
           OR (ub.blocker_id = r.user_id AND ub.blocked_id = :currentUserId)
    )
    """,
            nativeQuery = true)
    Page<GameRecord> searchRecordsWithAccessAndBlockFilter(
            @Param("query") String query,
            @Param("currentUserId") Long currentUserId,
            @Param("followingUserIds") String followingUserIds,
            Pageable pageable
    );

    @Query("SELECT gr FROM GameRecord gr JOIN FETCH gr.game g WHERE gr.userId = :userId AND g.date BETWEEN :startDate AND :endDate")
    List<GameRecord> findByUserIdAndGameDateBetween(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT count(gr) FROM GameRecord gr JOIN gr.game g WHERE gr.userId = :userId AND (g.homeTeam = :favTeam OR g.awayTeam = :favTeam)")
    long countByUserIdAndFavTeam(@Param("userId") Long userId, @Param("favTeam") String favTeam);

    long countByUserIdAndResult(Long userId, String result);

    @Query("SELECT DISTINCT gr.stadium FROM GameRecord gr WHERE gr.userId = :userId")
    Set<String> findDistinctStadiumsByUserId(@Param("userId") Long userId);

    @Query("SELECT gr FROM GameRecord gr JOIN FETCH gr.game WHERE gr.userId = :userId ORDER BY gr.createdAt DESC")
    List<GameRecord> findByUserIdWithDetails(@Param("userId") Long userId);
    @Query("SELECT gr FROM GameRecord gr " +
            "LEFT JOIN FETCH gr.game " +
            "WHERE gr.recordId = :recordId")
    Optional<GameRecord> findByIdWithDetails(@Param("recordId") Long recordId);
}
