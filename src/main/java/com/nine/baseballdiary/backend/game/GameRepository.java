package com.nine.baseballdiary.backend.game;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameRepository extends JpaRepository<Game, String> {
    // 추가적으로 필요한 쿼리 메서드가 있으면 선언
    List<Game> findByStatus(String status);
}
