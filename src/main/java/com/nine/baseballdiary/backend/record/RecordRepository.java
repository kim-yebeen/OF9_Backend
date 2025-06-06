package com.nine.baseballdiary.backend.record;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.List;

public interface RecordRepository extends JpaRepository<Record, Long> {


    // 인기순 정렬 (Native Query - PostgreSQL 최적화)
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
    List<Record> findAllFeedRecordsByPopularity(
            @Param("currentUserId") Long currentUserId,
            @Param("followingIds") String followingIds,  // 배열을 문자열로 전달
            @Param("date") String date,
            @Param("team") String team,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    // 최신순 정렬 (JPQL)
    @Query("""
        SELECT r FROM Record r 
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
    List<Record> findAllFeedRecordsByLatest(
            @Param("currentUserId") Long currentUserId,
            @Param("followingIds") List<Long> followingIds,
            @Param("date") LocalDate date,
            @Param("team") String team,
            Pageable pageable
    );


    // 팔로잉 피드 - 인기순
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
    List<Record> findFollowingFeedRecordsByPopularity(
            @Param("userIds") String userIds,
            @Param("date") String date,
            @Param("team") String team,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    // 팔로잉 피드 - 최신순
    @Query("""
        SELECT r FROM Record r 
        JOIN Game g ON r.gameId = g.gameId 
        WHERE r.userId IN :userIds
        AND g.date = :date
        AND (:team IS NULL OR g.homeTeam = :team OR g.awayTeam = :team)
        ORDER BY r.createdAt DESC
        """)
    List<Record> findFollowingFeedRecordsByLatest(
            @Param("userIds") List<Long> userIds,
            @Param("date") LocalDate date,
            @Param("team") String team,
            Pageable pageable
    );

    // 게시글 수 계산
    long countByUserId(Long userId);

    List<Record> findByUserId(Long userId);
}