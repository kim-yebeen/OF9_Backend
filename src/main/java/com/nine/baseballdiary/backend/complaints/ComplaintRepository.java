package com.nine.baseballdiary.backend.complaints;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    // 특정 사용자가 받은 신고 횟수
    long countByReportedUserId(Long reportedUserId);

    // 특정 게시글이 받은 신고 횟수
    long countByReportedRecordId(Long reportedRecordId);

    // 중복 신고 확인 (같은 날 같은 대상을 신고했는지)
    @Query("SELECT COUNT(c) > 0 FROM Complaint c " +
            "WHERE c.reporter.id = :reporterId " +
            "AND (:reportedUserId IS NULL OR c.reportedUser.id = :reportedUserId) " +
            "AND (:reportedRecordId IS NULL OR c.reportedRecord.id = :reportedRecordId) " +
            "AND DATE(c.createdAt) = CURRENT_DATE")
    boolean existsTodayComplaint(
            @Param("reporterId") Long reporterId,
            @Param("reportedUserId") Long reportedUserId,
            @Param("reportedRecordId") Long reportedRecordId
    );
}