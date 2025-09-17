package com.nine.baseballdiary.backend.record;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.List;

public interface GameRecordRepository extends JpaRepository<GameRecord, Long> {

    // 기존 메서드들 (차단 로직 미적용 - 하위 호환성)
    @Query(value = """
        SELECT r.* FROM record r 
        JOIN game g ON r.game_id = g.game_id 
        JOIN users u ON r.user_id = u.id 
        LEFT JOIN (
            SELECT rr.record_id, COUNT(rr.id) as reaction_count 
            FROM record_reaction rr 
            GROUP BY rr.record_id
        ) rc ON r.record_id = rc.record_id
        LEFT JOIN (
            SELECT uf.followee_id, COUNT(uf.follower_id) as follower_count 
            FROM user_follow uf 
            GROUP BY uf.followee_id
        ) fc ON u.id = fc.followee_id
        WHERE (
            u.is_private = false OR 
            r.user_id = :currentUserId OR 
            r.user_id = ANY(CAST(:followingIds AS bigint[]))
        )
        AND g.date = CAST(:date AS DATE)
        AND (:team IS NULL OR g.home_team = :team OR g.away_team = :team)
        ORDER BY 
            COALESCE(rc.reaction_count, 0) DESC,
            COALESCE(fc.follower_count, 0) DESC,
            u.nickname ASC
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<GameRecord> findAllFeedRecordsByPopularity(
            @Param("currentUserId") Long currentUserId,
            @Param("followingIds") String followingIds,
            @Param("date") String date,
            @Param("team") String team,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Query("""
        SELECT r FROM GameRecord r 
        JOIN Game g ON r.gameId = g.gameId 
        JOIN User u ON r.userId = u.id 
        WHERE (
            u.isPrivate = false OR 
            r.userId = :currentUserId OR 
            r.userId IN :followingIds
        )
        AND g.date = :date
        AND (:team IS NULL OR g.homeTeam = :team OR g.awayTeam = :team)
        ORDER BY r.createdAt DESC
        """)
    List<GameRecord> findAllFeedRecordsByLatest(
            @Param("currentUserId") Long currentUserId,
            @Param("followingIds") List<Long> followingIds,
            @Param("date") LocalDate date,
            @Param("team") String team,
            Pageable pageable
    );

    // 차단 사용자 제외 버전 (새로 추가)
    @Query(value = """
        SELECT r.* FROM record r 
        JOIN game g ON r.game_id = g.game_id 
        JOIN users u ON r.user_id = u.id 
        LEFT JOIN (
            SELECT rr.record_id, COUNT(rr.id) as reaction_count 
            FROM record_reaction rr 
            GROUP BY rr.record_id
        ) rc ON r.record_id = rc.record_id
        LEFT JOIN (
            SELECT uf.followee_id, COUNT(uf.follower_id) as follower_count 
            FROM user_follow uf 
            GROUP BY uf.followee_id
        ) fc ON u.id = fc.followee_id
        WHERE (
            u.is_private = false OR 
            r.user_id = :currentUserId OR 
            r.user_id = ANY(CAST(:followingIds AS bigint[]))
        )
        AND g.date = CAST(:date AS DATE)
        AND (:team IS NULL OR g.home_team = :team OR g.away_team = :team)
        AND NOT EXISTS (
            SELECT 1 FROM user_block ub 
            WHERE (ub.blocker_id = :currentUserId AND ub.blocked_id = r.user_id)
               OR (ub.blocker_id = r.user_id AND ub.blocked_id = :currentUserId)
        )
        ORDER BY 
            COALESCE(rc.reaction_count, 0) DESC,
            COALESCE(fc.follower_count, 0) DESC,
            u.nickname ASC
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<GameRecord> findAllFeedRecordsByPopularityWithBlockFilter(
            @Param("currentUserId") Long currentUserId,
            @Param("followingIds") String followingIds,
            @Param("date") String date,
            @Param("team") String team,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Query("""
        SELECT r FROM GameRecord r 
        JOIN Game g ON r.gameId = g.gameId 
        JOIN User u ON r.userId = u.id 
        WHERE (
            u.isPrivate = false OR 
            r.userId = :currentUserId OR 
            r.userId IN :followingIds
        )
        AND g.date = :date
        AND (:team IS NULL OR g.homeTeam = :team OR g.awayTeam = :team)
        AND NOT EXISTS (
            SELECT 1 FROM UserBlock ub 
            WHERE (ub.blocker.id = :currentUserId AND ub.blocked.id = r.userId)
               OR (ub.blocker.id = r.userId AND ub.blocked.id = :currentUserId)
        )
        ORDER BY r.createdAt DESC
        """)
    List<GameRecord> findAllFeedRecordsByLatestWithBlockFilter(
            @Param("currentUserId") Long currentUserId,
            @Param("followingIds") List<Long> followingIds,
            @Param("date") LocalDate date,
            @Param("team") String team,
            Pageable pageable
    );

    // 팔로잉 피드 - 인기순 (기존)
    @Query(value = """
        SELECT r.* FROM record r 
        JOIN game g ON r.game_id = g.game_id 
        LEFT JOIN (
            SELECT rr.record_id, COUNT(rr.id) as reaction_count 
            FROM record_reaction rr 
            GROUP BY rr.record_id
        ) rc ON r.record_id = rc.record_id
        LEFT JOIN users u ON r.user_id = u.id
        LEFT JOIN (
            SELECT uf.followee_id, COUNT(uf.follower_id) as follower_count 
            FROM user_follow uf 
            GROUP BY uf.followee_id
        ) fc ON u.id = fc.followee_id
        WHERE r.user_id = ANY(CAST(:userIds AS bigint[]))
        AND g.date = CAST(:date AS DATE)
        AND (:team IS NULL OR g.home_team = :team OR g.away_team = :team)
        ORDER BY 
            COALESCE(rc.reaction_count, 0) DESC,
            COALESCE(fc.follower_count, 0) DESC,
            u.nickname ASC
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<GameRecord> findFollowingFeedRecordsByPopularity(
            @Param("userIds") String userIds,
            @Param("date") String date,
            @Param("team") String team,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    // 팔로잉 피드 - 최신순 (기존)
    @Query("""
        SELECT r FROM GameRecord r 
        JOIN Game g ON r.gameId = g.gameId 
        WHERE r.userId IN :userIds
        AND g.date = :date
        AND (:team IS NULL OR g.homeTeam = :team OR g.awayTeam = :team)
        ORDER BY r.createdAt DESC
        """)
    List<GameRecord> findFollowingFeedRecordsByLatest(
            @Param("userIds") List<Long> userIds,
            @Param("date") LocalDate date,
            @Param("team") String team,
            Pageable pageable
    );

    // 팔로잉 피드 - 차단 사용자 제외 버전 (새로 추가)
    @Query(value = """
        SELECT r.* FROM record r 
        JOIN game g ON r.game_id = g.game_id 
        LEFT JOIN (
            SELECT rr.record_id, COUNT(rr.id) as reaction_count 
            FROM record_reaction rr 
            GROUP BY rr.record_id
        ) rc ON r.record_id = rc.record_id
        LEFT JOIN users u ON r.user_id = u.id
        LEFT JOIN (
            SELECT uf.followee_id, COUNT(uf.follower_id) as follower_count 
            FROM user_follow uf 
            GROUP BY uf.followee_id
        ) fc ON u.id = fc.followee_id
        WHERE r.user_id = ANY(CAST(:userIds AS bigint[]))
        AND g.date = CAST(:date AS DATE)
        AND (:team IS NULL OR g.home_team = :team OR g.away_team = :team)
        AND NOT EXISTS (
            SELECT 1 FROM user_block ub 
            WHERE (ub.blocker_id = :currentUserId AND ub.blocked_id = r.user_id)
               OR (ub.blocker_id = r.user_id AND ub.blocked_id = :currentUserId)
        )
        ORDER BY 
            COALESCE(rc.reaction_count, 0) DESC,
            COALESCE(fc.follower_count, 0) DESC,
            u.nickname ASC
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<GameRecord> findFollowingFeedRecordsByPopularityWithBlockFilter(
            @Param("userIds") String userIds,
            @Param("currentUserId") Long currentUserId,
            @Param("date") String date,
            @Param("team") String team,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Query("""
        SELECT r FROM GameRecord r 
        JOIN Game g ON r.gameId = g.gameId 
        WHERE r.userId IN :userIds
        AND g.date = :date
        AND (:team IS NULL OR g.homeTeam = :team OR g.awayTeam = :team)
        AND NOT EXISTS (
            SELECT 1 FROM UserBlock ub 
            WHERE (ub.blocker.id = :currentUserId AND ub.blocked.id = r.userId)
               OR (ub.blocker.id = r.userId AND ub.blocked.id = :currentUserId)
        )
        ORDER BY r.createdAt DESC
        """)
    List<GameRecord> findFollowingFeedRecordsByLatestWithBlockFilter(
            @Param("userIds") List<Long> userIds,
            @Param("currentUserId") Long currentUserId,
            @Param("date") LocalDate date,
            @Param("team") String team,
            Pageable pageable
    );

    // 게시글 수 계산
    long countByUserId(Long userId);

    List<GameRecord> findByUserId(Long userId);

    // 검색 기능 (기존)
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

    // 검색 기능 - 차단 사용자 제외 버전 (새로 추가)
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
}