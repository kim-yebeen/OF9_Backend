package com.nine.baseballdiary.backend.record;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.List;

public interface RecordRepository extends JpaRepository<Record, Long> {



    // 전체 피드: 공개 게시물 + 내가 팔로우한 비공개 계정 + 내 게시물
    @Query("""
        SELECT r FROM Record r 
        JOIN Game g ON r.gameId = g.gameId 
        JOIN User u ON r.userId = u.id 
        WHERE (
            u.isPrivate = false OR 
            r.userId = :currentUserId OR 
            r.userId IN :followingIds
        )
        AND (:date IS NULL OR g.date = :date)
        AND (:team IS NULL OR g.homeTeam = :team OR g.awayTeam = :team)
        ORDER BY r.createdAt DESC
        """)
    List<Record> findAllFeedRecords(
            @Param("currentUserId") Long currentUserId,
            @Param("followingIds") List<Long> followingIds,
            @Param("date") LocalDate date,
            @Param("team") String team,
            Pageable pageable
    );

    // 팔로잉 피드: 내가 팔로우한 사람들 + 나 자신
    @Query("""
        SELECT r FROM Record r 
        JOIN Game g ON r.gameId = g.gameId 
        WHERE r.userId IN :userIds
        AND (:date IS NULL OR g.date = :date)
        AND (:team IS NULL OR g.homeTeam = :team OR g.awayTeam = :team)
        ORDER BY r.createdAt DESC
        """)
    List<Record> findFollowingFeedRecords(
            @Param("userIds") List<Long> userIds,
            @Param("date") LocalDate date,
            @Param("team") String team,
            Pageable pageable
    );

    // 게시글 수 계산
    long countByUserId(Long userId);

    List<Record> findByUserId(Long userId);
}