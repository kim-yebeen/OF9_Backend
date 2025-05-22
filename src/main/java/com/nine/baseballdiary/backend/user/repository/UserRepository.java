package com.nine.baseballdiary.backend.user.repository;

import com.nine.baseballdiary.backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByKakaoId(Long kakaoId);

    boolean existsByNickname(String nickname);
    Optional<User> findByNickname(String nickname);
    List<User> findByNicknameContainingIgnoreCase(String q);
}

