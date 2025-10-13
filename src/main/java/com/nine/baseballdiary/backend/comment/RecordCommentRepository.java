package com.nine.baseballdiary.backend.comment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RecordCommentRepository extends JpaRepository<RecordComment, Long> {

    // 특정 게시물의 모든 댓글 조회 (삭제되지 않은 것만, 최신순)
    @Query("""
        SELECT c FROM RecordComment c
        WHERE c.recordId = :recordId 
        AND c.deletedAt IS NULL
        ORDER BY c.createdAt ASC
    """)
    List<RecordComment> findAllByRecordIdNotDeleted(@Param("recordId") Long recordId);

    // 특정 댓글 조회 (삭제되지 않은 것만)
    @Query("""
        SELECT c FROM RecordComment c
        WHERE c.id = :commentId 
        AND c.deletedAt IS NULL
    """)
    Optional<RecordComment> findByIdNotDeleted(@Param("commentId") Long commentId);

    // 특정 게시물의 댓글 개수 (삭제되지 않은 것만)
    long countByRecordIdAndDeletedAtIsNull(Long recordId);

    // 특정 댓글의 대댓글 개수 (삭제되지 않은 것만)
    long countByParentCommentIdAndDeletedAtIsNull(Long parentCommentId);
}