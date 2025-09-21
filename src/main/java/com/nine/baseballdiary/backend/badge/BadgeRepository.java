package com.nine.baseballdiary.backend.badge;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BadgeRepository extends JpaRepository<Badge, Integer> {
    List<Badge> findAllByOrderByCategory();
}