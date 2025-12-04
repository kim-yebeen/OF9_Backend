package com.nine.baseballdiary.backend.emotion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmotionCategoryRepository extends JpaRepository<EmotionCategory, Long> {

    /**
     * 특정 카테고리의 감정 목록 조회 (표시 순서대로)
     */
    @Query("SELECT ec FROM EmotionCategory ec " +
            "JOIN FETCH ec.emotion " +
            "WHERE ec.category = :category " +
            "ORDER BY ec.displayOrder ASC")
    List<EmotionCategory> findByCategoryOrderByDisplayOrder(@Param("category") String category);
}