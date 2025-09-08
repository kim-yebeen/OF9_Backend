package com.nine.baseballdiary.backend.user.repository;

import com.nine.baseballdiary.backend.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Pageable;  //
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByKakaoId(Long kakaoId);

    boolean existsByNickname(String nickname);
    Optional<User> findByNickname(String nickname);
    List<User> findByNicknameContainingIgnoreCase(String q);

    // [추가] id 리스트로 조회
    List<User> findByIdIn(List<Long> ids);

    // [추가] id 리스트 + 닉네임 검색
    List<User> findByIdInAndNicknameContainingIgnoreCase(List<Long> ids, String nickname);

    // 검색 기능을 위한 메서드 추가 (자신 제외하고 페이징)
    Page<User> findByNicknameContainingIgnoreCaseAndIdNot(String nickname, Long excludeUserId, Pageable pageable);



    @Query(value = """
    SELECT u FROM User u
    WHERE LOWER(u.nickname) LIKE LOWER(CONCAT('%', :nickname, '%'))
    AND u.id != :currentUserId
    AND NOT EXISTS (
        SELECT 1 FROM UserBlock ub
        WHERE (ub.blocker.id = :currentUserId AND ub.blocked.id = u.id)
           OR (ub.blocker.id = u.id AND ub.blocked.id = :currentUserId)
    )
    """,
            countQuery = """
    SELECT count(u) FROM User u
    WHERE LOWER(u.nickname) LIKE LOWER(CONCAT('%', :nickname, '%'))
    AND u.id != :currentUserId
    AND NOT EXISTS (
        SELECT 1 FROM UserBlock ub
        WHERE (ub.blocker.id = :currentUserId AND ub.blocked.id = u.id)
           OR (ub.blocker.id = u.id AND ub.blocked.id = :currentUserId)
    )
    """)
    Page<User> findByNicknameContainingIgnoreCaseAndIdNotExcludingBlocked(
            @Param("nickname") String nickname,
            @Param("currentUserId") Long currentUserId,
            Pageable pageable
    );


    // 특정 사용자 목록에서 차단된 사용자 제외
    @Query("""
    SELECT u FROM User u 
    WHERE u.id IN :userIds
    AND NOT EXISTS (
        SELECT 1 FROM UserBlock ub 
        WHERE (ub.blocker.id = :currentUserId AND ub.blocked.id = u.id)
           OR (ub.blocker.id = u.id AND ub.blocked.id = :currentUserId)
    )
    """)
    List<User> findByIdInExcludingBlocked(
            @Param("userIds") List<Long> userIds,
            @Param("currentUserId") Long currentUserId
    );
}

