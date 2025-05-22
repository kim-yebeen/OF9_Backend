package com.nine.baseballdiary.backend.record;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RecordRepository extends JpaRepository<Record, Long> {



    // 게시글 수 계산
    long countByUserId(Long userId);

    List<Record> findByUserId(Long userId);
}