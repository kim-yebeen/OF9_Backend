package com.nine.baseballdiary.backend.reaction;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReactionTypeRepository extends JpaRepository<ReactionType, Long> {
    List<ReactionType> findAllByOrderByDisplayOrder();
}